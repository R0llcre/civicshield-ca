# Change Log

Date: 2026-02-13

Owner: Codex (branch: `codex/ask-invalid-request-fix`)

Topic: Fix Android Ask "Invalid request" by normalizing roles + sanitizing stored chat; add server-side tolerant parsing for locale/role casing.

## Why

- Android Ask was returning `Invalid request` (HTTP 400) from `/api/ask` even though Azure staging `/api/ask` works.
- Root cause: the server request schema requires `messages[].role` to be exactly `"user"` or `"assistant"` (lowercase) and `locale` to be one of `en|es|zh-Hans`.
- Older app builds or persisted chat data can contain roles like `"User"`/`"Assistant"`; the UI lowercased for display, but the API payload kept the original casing, causing server-side validation to fail.

## What Changed

### Android

- Normalize and filter roles when building the API payload, so the client always sends valid roles.
- Sanitize persisted chat history on read (normalize casing; drop invalid roles/blank messages) and write back once, so users do not need to manually clear chat to recover.

Files:
- `/Users/shenkairan/Documents/RegShield CA/apps/android/app/src/main/java/org/regshield/ca/ui/screens/ask/AskViewModel.kt`

### Web (Server)

- Make `/api/ask` request parsing more tolerant:
  - Accept role casing differences by normalizing to lowercase before validation.
  - Accept common locale tags (e.g. `en-US`) by normalizing to `en|es|zh-Hans`.
- Still enforces the same security boundary: only `user|assistant` roles are accepted; no `system` role is permitted.

Files:
- `/Users/shenkairan/Documents/RegShield CA/apps/web/src/app/api/ask/route.ts`
- `/Users/shenkairan/Documents/RegShield CA/apps/web/src/app/api/ask/route.test.ts`

## How To Verify (Commands + Results)

Web:

```bash
cd "/Users/shenkairan/Documents/RegShield CA"
npm -w apps/web run lint
npm -w apps/web run typecheck
npm -w apps/web run test
```

Android:

```bash
cd "/Users/shenkairan/Documents/RegShield CA"
JAVA_HOME=$(/usr/libexec/java_home -v 17) ANDROID_SDK_ROOT="$HOME/Library/Android/sdk" \
./gradlew :app:test :app:assembleStagingDebug
```

Azure staging smoke:

```bash
curl -fsS -X POST "https://ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io/api/ask" \
  -H "content-type: application/json" \
  -d '{"locale":"en","messages":[{"role":"user","content":"How do I verify an accredited representative in California?"}]}'
```

Expected:
- Android no longer shows `Invalid request` for previously persisted chats.
- Server accepts `role: "User"` and `locale: "en-US"` (normalizes) without returning 400.

## How To Demo (Step-by-step)

1. Launch staging Android app (emulator):

```bash
cd "/Users/shenkairan/Documents/RegShield CA"
npm run android:staging:up
```

2. In the emulator:
- Go to `Ask`
- Send: "How do I verify an accredited representative in California?"
- Expected: answer renders with citations + suggested actions; no `Invalid request`.

## Rollback / Recovery

- Android: revert the commit(s) on the branch and reinstall the app.
- Web: revert the route schema change (or deploy previous ACA revision in staging).

