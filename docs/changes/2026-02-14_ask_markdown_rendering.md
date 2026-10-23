# 2026-02-14 - Ask Markdown Rendering (Android)

Date: 2026-02-14

Owner: Codex

Topic: Render `/api/ask` `answer_markdown` as rich text on Android (full Markdown), with safe link allowlist.

## Why

- Problem: Ask answers are returned as Markdown (`**bold**`, lists, `[text](url)` links). Android was rendering them as plain text (`Text(msg.content)`), so the UI showed raw Markdown markers and looked broken/unpolished.
- Tradeoff: We chose a full Markdown renderer (Markwon) instead of simplifying model output, because we want to keep rich formatting.
- Safety: Inline links are restricted to the same allowlisted domains as the server to avoid opening untrusted sources.

## What Changed

- Android: Add Markwon dependencies for Markdown rendering.
  - `<repo-root>/apps/android/app/build.gradle.kts`
- Android: Add a reusable `MarkdownText` composable (Compose `AndroidView(TextView)` + Markwon).
  - `<repo-root>/apps/android/app/src/main/java/org/regshield/ca/ui/components/MarkdownText.kt`
- Android: Ask assistant bubbles render Markdown via `MarkdownText` (user bubbles remain plain text).
  - `<repo-root>/apps/android/app/src/main/java/org/regshield/ca/ui/screens/ask/AskScreen.kt`
- Android: Add androidTest to assert we do not display raw Markdown markers.
  - `<repo-root>/apps/android/app/src/androidTest/java/org/regshield/ca/ui/MarkdownRenderTest.kt`
- Docs: Update engineering checklist with Ask Markdown acceptance points.
  - `<repo-root>/docs/ENGINEERING_CHECKLIST.md`

Behavior changes:
- Ask assistant messages now render rich Markdown (bold/lists/links/tables/code blocks).
- Inline links only open if they match the allowlist; otherwise they are blocked with a short toast.

## How To Verify (Commands + Results)

Web (always):

```bash
cd "<repo-root>"
npm -w apps/web run lint
npm -w apps/web run typecheck
npm -w apps/web run test
```

Android (build + unit tests + androidTest build):

```bash
cd "<repo-root>"
JAVA_HOME=$(/usr/libexec/java_home -v 17) ANDROID_SDK_ROOT="$HOME/Library/Android/sdk" \
./gradlew :app:test :app:assembleStagingDebug :app:assembleDevDebugAndroidTest
```

Staging smoke:

```bash
STG="https://ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io"
curl -fsS "$STG/api/meta/health" | jq
curl -fsS -X POST "$STG/api/ask" -H "content-type: application/json" \
  -d '{"messages":[{"role":"user","content":"How do I verify an accredited representative in California?"}]}' \
  | jq '{ok:(.answer_markdown|length>0), citations_count:(.citations|length)}'
```

Results:
- `npm -w apps/web run lint` PASS
- `npm -w apps/web run typecheck` PASS
- `npm -w apps/web run test` PASS
- `./gradlew :app:test :app:assembleStagingDebug :app:assembleDevDebugAndroidTest` PASS
- Staging smoke:
  - `/api/meta/health`: `ok=true`, `mode=postgres`, `ask_configured=true`, `rag_configured=true`
  - `/api/ask`: returns `answer_markdown` with allowlisted citations

## How To Demo (Step-by-step)

Android:
1. Install + launch staging build:

```bash
cd "<repo-root>"
npm run android:staging:up
```

2. In the emulator:
- Open `Ask`
- Send: `How do I verify an accredited representative in California?`
- Verify:
  - No raw Markdown markers (`**`, `[text](url)`) are visible.
  - Lists/paragraph spacing look correct.
  - Inline links (if present) are clickable only for allowlisted domains.
  - `Sources` buttons still work and open official sites.
3. Verified installer output:
   - AVD: `Pixel_8_Pro_Edited_API_34`
   - Serial: `emulator-5554`
   - Variant: `stagingDebug`

## Rollback / Recovery

Android:
- Revert this change and rebuild/install:
  - `git revert <commit>`
  - `npm run android:staging:up`

Web (Azure Container Apps):
- No server change required for this feature.

## Notes

- We intentionally did not enable image loading for Markdown (`![]()`), to avoid fetching external resources automatically.
