# Jetnote managed AI proxy

The HTTP function `aiProxy` is the only path used for requests that rely on
Jetnote's managed SiliconFlow key. Requests carrying a user's own provider key
remain direct and are not charged against this managed-key quota.

## Security model

1. The Android client obtains a Firebase Authentication ID token. If no Firebase
   user exists, it creates an anonymous Firebase session.
2. The function verifies the token with Firebase Admin Auth (including revoked
   token checks) and derives the quota key exclusively from the verified UID.
   No UID supplied in a request body or header is trusted.
3. A Firestore transaction increments one per-UID fixed-window counter. The
   transaction is atomic across concurrent function instances.
4. The SiliconFlow API key is stored as a Firebase Functions secret and is never
   returned to the Android client.
5. The function proxies SSE response chunks as they arrive, so streaming chat
   completions are not buffered until the provider finishes.

Default limits are 20 requests per minute and 300 requests per UTC day per
Firebase UID. Override them with `AI_RATE_LIMIT_PER_MINUTE` and
`AI_RATE_LIMIT_PER_DAY`. The server-side model allowlist is configured with
`AI_PROXY_ALLOWED_MODELS`; the Remote Config list is only a UI/client hint.

Anonymous Firebase identities are stable across normal app restarts, but can be
reset by clearing app data or reinstalling. For stronger anti-abuse guarantees,
link anonymous sessions to a verified sign-in provider and enable Firebase App
Check before increasing quotas.

## Firebase setup

Run these commands from the repository root:

```sh
cd functions
npm install
cp .env.example .env
# Edit .env and set AI_PROXY_ALLOWED_MODELS to the exact free model IDs.
cd ..
firebase use jetnote-cloud-service
firebase functions:secrets:set SILICONFLOW_API_KEY
firebase deploy --only functions:aiProxy
```

Enable **Authentication → Sign-in method → Anonymous** in the Firebase console.
Create/enable the project's Cloud Firestore database in the same project and
region. The quota store uses the `aiRateLimits` collection through the Admin
SDK; client SDKs must not be allowed to read or write it. Add this match block
to the project's existing Firestore rules (do not replace unrelated rules):

```text
match /aiRateLimits/{quotaId} {
  allow read, write: if false;
}
```

If Firestore TTL is enabled for the project, configure TTL on the `expireAt`
field in `aiRateLimits` to clean up inactive quota records.

After deploying, remove `silicon_cloud_api_key` from Firebase Remote Config and
rotate the old SiliconFlow key: the previous Base64-encoded value was recoverable
from the client app and must be treated as compromised.

## Response contract

- `401`: missing, invalid, expired, or revoked Firebase ID token.
- `403`: requested model is not in the server allowlist.
- `429`: per-minute or daily UID quota exceeded; includes `Retry-After`.
- `503`: quota storage or server configuration unavailable.
- `502`: provider unavailable.

The function accepts only `GET /v1/models` and
`POST /v1/chat/completions`. It limits request bodies to 1 MiB and forwards
provider SSE chunks without logging prompts, tokens, or provider credentials.
