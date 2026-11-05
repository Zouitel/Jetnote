import assert from "node:assert/strict";
import test from "node:test";
import { evaluateRateLimit } from "../src/rateLimit";

const minute = 60_000;
const day = 86_400_000;

test("increments per-user minute and daily counters atomically", () => {
  const result = evaluateRateLimit(undefined, 10_000, 2, 5);
  assert.equal(result.allowed, true);
  assert.equal(result.state.minuteCount, 1);
  assert.equal(result.state.dayCount, 1);
});

test("resets the minute counter while retaining the daily counter", () => {
  const now = minute + 1_000;
  const result = evaluateRateLimit(
    { minuteWindow: 0, minuteCount: 2, dayWindow: 0, dayCount: 4 },
    now,
    2,
    5
  );
  assert.equal(result.allowed, true);
  assert.equal(result.state.minuteCount, 1);
  assert.equal(result.state.dayCount, 5);
});

test("rejects requests over the minute limit and supplies Retry-After", () => {
  const now = 12_000;
  const result = evaluateRateLimit(
    { minuteWindow: 0, minuteCount: 2, dayWindow: 0, dayCount: 2 },
    now,
    2,
    10
  );
  assert.equal(result.allowed, false);
  assert.equal(result.retryAfterSeconds, 48);
});

test("rejects requests over the daily limit", () => {
  const now = 2 * day + 10_000;
  const result = evaluateRateLimit(
    { minuteWindow: Math.floor(now / minute), minuteCount: 0, dayWindow: 2, dayCount: 3 },
    now,
    10,
    3
  );
  assert.equal(result.allowed, false);
  assert.equal(result.retryAfterSeconds, 86_390);
});
