# Change Log

Date: 2026-02-13

Owner: Codex (branch: `codex/ask-invalid-request-fix`)

Topic: Fix Ask `Invalid request` in English locale by ensuring `locale` is always sent; add server default for missing locale.

## Why

- In the Android Ask screen (English UI), requests to `/api/ask` were returning HTTP 400 with `{ "error": "Invalid request" }`.
- Root cause: Kotlinx serialization may omit properties that equal their default values when `encodeDefaults=false` (default behavior). If `AskRequest.locale` defaults to `"en"` and the device locale is English, the serialized JSON can omit `locale` entirely.
- The server request schema required `locale`, so missing `locale` caused validation failure.

## What Changed

### Web (Server)

- `/api/ask` now treats missing `locale` as `"en"` (compatibility for clients that omit default values).
- Still normalizes locale tags (e.g. `en-US`) and role casing (e.g. `User` -> `user`) and still only allows `user|assistant` roles.

Files:
- `<repo-root>/apps/web/src/app/api/ask/route.ts`
- `<repo-root>/apps/web/src/app/api/ask/route.test.ts`

### Android

- Removed defaults from `AskRequest` so `locale` is always encoded in JSON even when the value is `"en"`.

Files:
- `<repo-root>/apps/android/app/src/main/java/org/regshield/ca/api/Models.kt`

## How To Verify (Commands + Results)

Web:

```bash
cd "<repo-root>"
npm -w apps/web run lint
npm -w apps/web run typecheck
npm -w apps/web run test
```

Android:

```bash
cd "<repo-root>"
JAVA_HOME=$(/usr/libexec/java_home -v 17) ANDROID_SDK_ROOT="$HOME/Library/Android/sdk" \
./gradlew :app:test :app:assembleStagingDebug
```

Staging smoke (must both succeed):

```bash
curl -fsS -X POST "https://ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io/api/ask" \
  -H "content-type: application/json" \
  -d '{"messages":[{"role":"user","content":"How do I verify an accredited representative in California?"}]}'

curl -fsS -X POST "https://ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io/api/ask" \
  -H "content-type: application/json" \
  -d '{"locale":"en","messages":[{"role":"user","content":"How do I verify an accredited representative in California?"}]}'
```

Expected:
- No `Invalid request` in English locale.

## How To Demo (Step-by-step)

1. Install and launch Android staging app:

```bash
cd "<repo-root>"
npm run android:staging:up
```

2. In the emulator:
- Go to `Ask`
- Send: "How do I verify an accredited representative in California?"
- Expected: answer renders; no `Invalid request`.

## Rollback / Recovery

- Android: revert the commit(s) and reinstall the app.
- Web (staging): activate the previous Container Apps revision:

```bash
az containerapp revision list -g rg-regshieldca-stg-westus2 -n ca-regshieldca-web-stg -o table
az containerapp revision activate -g rg-regshieldca-stg-westus2 -n ca-regshieldca-web-stg --revision <REVISION_NAME>
```

