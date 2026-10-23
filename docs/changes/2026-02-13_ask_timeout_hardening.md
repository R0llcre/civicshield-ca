# Change Log

Date: 2026-02-13

Owner: Codex (branch: `codex/ask-invalid-request-fix`)

Topic: Reduce Ask flakiness by increasing timeouts for slow RAG calls and improving client error messaging.

## Why

- Ask calls are noticeably slower than other endpoints because they involve RAG/tool calls and model generation.
- We observed `/api/ask` latencies in the 10–18s range, which can occasionally exceed the previous 20s server/client timeouts, causing transient failures.
- In Android, timeouts surfaced as a generic "AI request failed" message and felt like the app "hung" while sending.

## What Changed

### Web (Server)

- Increased OpenAI request timeout from 20s to 45s to avoid aborting slow RAG responses.

Files:
- `<repo-root>/apps/web/src/server/openai.ts`

### Android

- Increased OkHttp timeouts to better accommodate slow `/api/ask` responses:
  - readTimeout: 45s
  - writeTimeout: 30s
  - callTimeout: 50s
- Improved error message for network timeouts (SocketTimeoutException) to be actionable.

Files:
- `<repo-root>/apps/android/app/src/main/java/org/regshield/ca/di/NetworkModule.kt`
- `<repo-root>/apps/android/app/src/main/java/org/regshield/ca/ui/screens/ask/AskViewModel.kt`

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

Staging deploy + Ask smoke:

```bash
cd "<repo-root>"
bash scripts/azure_staging_deploy_web.sh
bash scripts/azure_staging_enable_ask_rag.sh
```

Expected:
- `/api/ask` succeeds reliably for typical questions.

## How To Demo (Step-by-step)

```bash
cd "<repo-root>"
npm run android:staging:up
```

In the emulator:
- Go to `Ask`
- Send: "How do I verify an accredited representative in California?"
- Expected: answer renders (may take ~10–20s). If a timeout occurs, the UI shows "Request timed out. Please try again."

## Rollback / Recovery

- Android: revert the commit(s) and reinstall.
- Web (staging): activate the previous Container Apps revision.

