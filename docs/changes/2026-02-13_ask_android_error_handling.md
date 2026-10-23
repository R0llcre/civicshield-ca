# Change Log

Date: 2026-02-13

Owner: Codex (branch: `codex/civicshield-glass-ask41`)

Topic: Fix Ask failures caused by oversized chat context and improve error visibility.

## Why

- Android Ask was showing a generic "AI request failed" even when the server returned a useful JSON error (Retrofit `body()` is null on non-2xx).
- After a long assistant answer, the next Ask request could exceed server limits (per-message and total message size), causing repeated failures until the user cleared chat.

## What Changed

- Server: give a clearer 400 error when the request is rejected due to length limits.
  - `<repo-root>/apps/web/src/app/api/ask/route.ts`

- Android: ensure requests stay under server limits and show real server errors.
  - `<repo-root>/apps/android/app/src/main/java/org/regshield/ca/ui/screens/ask/AskViewModel.kt`
    - Trim/slice message history by character budget before sending to `/api/ask`
    - Parse `errorBody()` and `retry-after` (429) so users see actionable errors

- Minor DX: script banner text uses the new app name.
  - `<repo-root>/scripts/android_staging_up.sh`

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

Azure staging:

```bash
cd "<repo-root>"
bash scripts/azure_staging_deploy_web.sh
bash scripts/azure_staging_enable_ask_rag.sh
curl -fsS -X POST "https://ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io/api/ask" \
  -H "content-type: application/json" \
  -d '{"locale":"en","messages":[{"role":"user","content":"How do I verify an accredited representative in California?"}]}' | jq
```

Expected:
- `/api/ask` returns 200 with `answer_markdown`, `citations`, and `suggested_actions`.

## How To Demo (Step-by-step)

1. Install and launch the Android staging app:

```bash
cd "<repo-root>"
npm run android:staging:up
```

2. In the emulator:
- Go to `Ask`
- Tap `Clear chat` (to remove any oversized stored history)
- Ask: "How do I verify an accredited representative in California?"
- You should see an answer and citations.

3. If you spam Send and hit rate limit:
- The UI should show a message like: "Rate limit exceeded. Please try again in XXs."

## Rollback / Recovery

- Android: revert commit(s) on the branch and reinstall.
- Web (staging): activate the previous ACA revision:

```bash
az containerapp revision list -g rg-regshieldca-stg-westus2 -n ca-regshieldca-web-stg -o table
az containerapp revision activate -g rg-regshieldca-stg-westus2 -n ca-regshieldca-web-stg --revision <REVISION_NAME>
```

