import { createHash } from "node:crypto";
import { initializeApp, getApps } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { defineSecret } from "firebase-functions/params";
import { logger } from "firebase-functions";
import { onRequest } from "firebase-functions/v2/https";
import { evaluateRateLimit, RateLimitState } from "./rateLimit";

if (getApps().length === 0) initializeApp();

const db = getFirestore();
const siliconFlowApiKey = defineSecret("SILICONFLOW_API_KEY");
const UPSTREAM_BASE_URL = "https://api.siliconflow.cn";
const MAX_BODY_BYTES = 1_048_576;
const UPSTREAM_TIMEOUT_MS = 8 * 60 * 1_000;

function positiveInteger(value: string | undefined, fallback: number): number {
  const parsed = Number(value);
  return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : fallback;
}

function getAllowedModels(): Set<string> {
  return new Set(
    (process.env.AI_PROXY_ALLOWED_MODELS ?? "")
      .split(",")
      .map((model) => model.trim())
      .filter(Boolean)
  );
}

function getRequestPath(originalUrl: string): { pathname: string; search: string } {
  const parsed = new URL(originalUrl, "https://jetnote.invalid");
  const pathname = parsed.pathname.replace(/^\/aiProxy(?=\/)/, "");
  return { pathname, search: parsed.search };
}

async function consumeUserQuota(uid: string): Promise<{ allowed: boolean; retryAfterSeconds: number; remainingMinute: number; remainingDay: number }> {
  const uidHash = createHash("sha256").update(uid).digest("hex");
  const ref = db.collection("aiRateLimits").doc(uidHash);
  const now = Date.now();
  const perMinute = positiveInteger(process.env.AI_RATE_LIMIT_PER_MINUTE, 20);
  const perDay = positiveInteger(process.env.AI_RATE_LIMIT_PER_DAY, 300);

  const decision = await db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(ref);
    const current = snapshot.exists ? snapshot.data() as Partial<RateLimitState> : undefined;
    const next = evaluateRateLimit(current, now, perMinute, perDay);

    if (next.allowed) {
      transaction.set(ref, {
        ...next.state,
        updatedAt: FieldValue.serverTimestamp(),
        expireAt: new Date(now + 7 * 86_400_000)
      });
    }
    return next;
  });

  return {
    allowed: decision.allowed,
    retryAfterSeconds: decision.retryAfterSeconds,
    remainingMinute: Math.max(0, perMinute - decision.state.minuteCount),
    remainingDay: Math.max(0, perDay - decision.state.dayCount)
  };
}

export const aiProxy = onRequest(
  {
    region: "us-central1",
    timeoutSeconds: 540,
    memory: "512MiB",
    concurrency: 40,
    maxInstances: 20,
    secrets: [siliconFlowApiKey]
  },
  async (req, res) => {
    res.set("Cache-Control", "no-store");

    const { pathname, search } = getRequestPath(req.originalUrl || req.url);
    const method = req.method.toUpperCase();
    const isModelsRequest = method === "GET" && pathname === "/v1/models";
    const isChatRequest = method === "POST" && pathname === "/v1/chat/completions";

    if (!isModelsRequest && !isChatRequest) {
      res.status(404).json({ error: { code: "NOT_FOUND", message: "Unsupported AI proxy route." } });
      return;
    }

    const authorization = req.get("authorization") ?? "";
    const tokenMatch = /^Bearer\s+([^\s]+)$/i.exec(authorization);
    if (!tokenMatch) {
      res.status(401).json({ error: { code: "UNAUTHENTICATED", message: "A valid Firebase ID token is required." } });
      return;
    }

    let uid: string;
    try {
      const decoded = await getAuth().verifyIdToken(tokenMatch[1], true);
      uid = decoded.uid;
    } catch {
      res.status(401).json({ error: { code: "UNAUTHENTICATED", message: "A valid Firebase ID token is required." } });
      return;
    }

    let body = Buffer.alloc(0);
    if (isChatRequest) {
      body = Buffer.from(req.rawBody ?? Buffer.from(JSON.stringify(req.body ?? {})));
      if (body.byteLength === 0 || body.byteLength > MAX_BODY_BYTES) {
        res.status(body.byteLength > MAX_BODY_BYTES ? 413 : 400).json({
          error: { code: "INVALID_BODY", message: "The AI request body is empty or too large." }
        });
        return;
      }

      let parsedBody: Record<string, unknown>;
      try {
        parsedBody = JSON.parse(body.toString("utf8")) as Record<string, unknown>;
      } catch {
        res.status(400).json({ error: { code: "INVALID_BODY", message: "The AI request body must be valid JSON." } });
        return;
      }

      const currentModel = parsedBody.currentModel;
      const model = typeof parsedBody.model === "string"
        ? parsedBody.model
        : typeof currentModel === "string"
          ? currentModel
          : currentModel && typeof currentModel === "object" && "id" in currentModel
            ? String((currentModel as { id: unknown }).id)
            : "";

      const allowedModels = getAllowedModels();
      if (allowedModels.size === 0) {
        res.status(503).json({ error: { code: "AI_PROXY_NOT_CONFIGURED", message: "The managed AI model allowlist is not configured." } });
        return;
      }
      if (!allowedModels.has(model)) {
        res.status(403).json({ error: { code: "MODEL_NOT_ALLOWED", message: "This model is not available through the managed AI service." } });
        return;
      }
    }

    let quota: Awaited<ReturnType<typeof consumeUserQuota>>;
    try {
      quota = await consumeUserQuota(uid);
    } catch (error) {
      logger.error("AI quota store unavailable", {
        errorName: error instanceof Error ? error.name : "UnknownError"
      });
      res.status(503).json({ error: { code: "QUOTA_UNAVAILABLE", message: "AI service is temporarily unavailable." } });
      return;
    }

    res.set("X-RateLimit-Limit-Minute", String(positiveInteger(process.env.AI_RATE_LIMIT_PER_MINUTE, 20)));
    res.set("X-RateLimit-Remaining-Minute", String(quota.remainingMinute));
    res.set("X-RateLimit-Remaining-Day", String(quota.remainingDay));

    if (!quota.allowed) {
      res.set("Retry-After", String(quota.retryAfterSeconds));
      res.status(429).json({
        error: {
          code: "RATE_LIMITED",
          message: "Your Jetnote AI request limit has been reached. Try again later.",
          retryAfterSeconds: quota.retryAfterSeconds
        }
      });
      return;
    }

    const upstreamUrl = new URL(pathname + search, UPSTREAM_BASE_URL);
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), UPSTREAM_TIMEOUT_MS);
    res.on("close", () => {
      if (!res.writableEnded) controller.abort();
    });

    try {
      const upstream = await fetch(upstreamUrl, {
        method,
        headers: {
          Authorization: `Bearer ${siliconFlowApiKey.value()}`,
          Accept: req.get("accept") ?? "application/json",
          ...(isChatRequest ? { "Content-Type": req.get("content-type") ?? "application/json" } : {})
        },
        ...(isChatRequest ? { body } : {}),
        redirect: "manual",
        signal: controller.signal
      });

      res.status(upstream.status);
      for (const header of ["content-type", "cache-control", "x-request-id", "retry-after"]) {
        const value = upstream.headers.get(header);
        if (value) res.set(header, value);
      }

      if (!upstream.body) {
        res.end();
        return;
      }

      // Preserve SSE/chunked responses instead of buffering a full model completion.
      for await (const chunk of upstream.body) {
        if (!res.write(chunk)) {
          await new Promise<void>((resolve) => res.once("drain", resolve));
        }
      }
      res.end();
    } catch (error) {
      logger.error("AI upstream request failed", {
        errorName: error instanceof Error ? error.name : "UnknownError"
      });
      if (!res.headersSent) {
        res.status(502).json({ error: { code: "AI_UPSTREAM_UNAVAILABLE", message: "The AI provider is temporarily unavailable." } });
      } else {
        res.end();
      }
    } finally {
      clearTimeout(timeout);
    }
  }
);
