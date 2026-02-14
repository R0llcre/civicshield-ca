# 2026-02-14 — Ask Multi-Chat Local History (Android + Web + iOS)

Date: 2026-02-14

Owner: shenkairan / Codex

Topic: Ask multi-chat threads (local-only), migration from legacy single chat

## Why

- Ask previously stored only a single chat history (up to 50 messages). Users wanted a ChatGPT-like experience: multiple chats, switching, rename/delete, export.
- We explicitly chose **local-only storage** (no server-side chat storage, no login) to preserve privacy and reduce compliance surface area.
- Server constraints remain unchanged: every request sends only the **most recent context** (max 20 messages / size limits); the full history is retained only for viewing on-device.

## What Changed

- Android
  - Added v2 multi-thread storage on DataStore and migrated legacy `chat_json` into one thread.
  - Added thread actions in Ask UI (Chats/New/Export/Clear/Delete) + thread list with search/rename/delete.
  - Added a short UI hint that long chats may only send the most recent context to the model.
  - Updated tests for new UI tags and added a unit test for meta building logic.
  - Files:
    - `/Users/shenkairan/Documents/RegShield CA/apps/android/app/src/main/java/org/regshield/ca/data/AskLocalStore.kt`
    - `/Users/shenkairan/Documents/RegShield CA/apps/android/app/src/main/java/org/regshield/ca/ui/screens/ask/AskViewModel.kt`
    - `/Users/shenkairan/Documents/RegShield CA/apps/android/app/src/main/java/org/regshield/ca/ui/screens/ask/AskScreen.kt`
    - `/Users/shenkairan/Documents/RegShield CA/apps/android/app/src/androidTest/java/org/regshield/ca/ui/AppUiFlowsTest.kt`
    - `/Users/shenkairan/Documents/RegShield CA/apps/android/app/src/test/java/org/regshield/ca/data/AskThreadsMetaTest.kt`

- Web
  - Introduced `askStorage.ts` (v2 threads index + per-thread messages), with v1 migration from locale-scoped single-chat keys.
  - Updated Ask UI to support Chats dialog + New chat + Export, plus rename/delete/confirm.
  - Added a short UI hint that long chats may only send the most recent context to the model.
  - Added storage unit tests and i18n keys for the new UI.
  - Files:
    - `/Users/shenkairan/Documents/RegShield CA/apps/web/src/features/ask/askStorage.ts`
    - `/Users/shenkairan/Documents/RegShield CA/apps/web/src/features/ask/askStorage.test.ts`
    - `/Users/shenkairan/Documents/RegShield CA/apps/web/src/features/ask/AskClient.tsx`
    - `/Users/shenkairan/Documents/RegShield CA/apps/web/messages/en.json`
    - `/Users/shenkairan/Documents/RegShield CA/apps/web/messages/es.json`
    - `/Users/shenkairan/Documents/RegShield CA/apps/web/messages/zh-Hans.json`

- iOS
  - Migrated Ask to multi-chat threads stored in `UserDefaults` (v2).
  - Added Chats sheet (search/open/rename/delete), New chat, Export, Clear/Delete current.
  - Added a short UI hint that long chats may only send the most recent context to the model.
  - Made `AskViewModel` injectable with `UserDefaults` for testability and added a migration unit test.
  - Files:
    - `/Users/shenkairan/Documents/RegShield CA/apps/ios/CivicShield/Sources/Features/Ask/AskViewModel.swift`
    - `/Users/shenkairan/Documents/RegShield CA/apps/ios/CivicShield/Sources/Features/Ask/AskView.swift`
    - `/Users/shenkairan/Documents/RegShield CA/apps/ios/CivicShield/Tests/AskThreadsMigrationTests.swift`

- Docs
  - Updated manual demo steps to include multi-chat creation/switch/export.
  - File:
    - `/Users/shenkairan/Documents/RegShield CA/docs/ENGINEERING_CHECKLIST.md`

## How To Verify (Commands + Results)

Web:

```bash
cd "/Users/shenkairan/Documents/RegShield CA"
npm -w apps/web run lint
npm -w apps/web run typecheck
npm -w apps/web run test
npm -w apps/web run i18n:check
```

Android:

```bash
cd "/Users/shenkairan/Documents/RegShield CA"
JAVA_HOME=$(/usr/libexec/java_home -v 17) ANDROID_SDK_ROOT="$HOME/Library/Android/sdk" \
./gradlew :app:test :app:assembleStagingDebug
```

iOS:

```bash
cd "/Users/shenkairan/Documents/RegShield CA/apps/ios/CivicShield"
xcodegen generate
xcodebuild test \
  -project CivicShield.xcodeproj \
  -scheme CivicShield-Staging \
  -destination "platform=iOS Simulator,name=iPhone 15 Pro Max,OS=17.5"
```

Results:
- Web: PASS (`lint/typecheck/test/i18n:check`)
- Android: PASS (`./gradlew :app:test :app:assembleStagingDebug`)
- iOS: PASS (`xcodegen generate` + `xcodebuild test ... CivicShield-Staging`)

## How To Demo (Step-by-step)

Web:
1) Open `https://<staging-host>/<locale>/ask` or `http://localhost:3000/<locale>/ask`.
2) Click `Chats` -> `New chat` (create chat A), send a question.
3) Click `Chats` -> `New chat` (create chat B), send a different question.
4) Switch back to chat A: history must still be there.
5) Export chat A: a `.txt` download should include citations.

Android:
1) `cd "/Users/shenkairan/Documents/RegShield CA" && npm run android:staging:up`
2) Go to `Ask` tab.
3) `Chats` -> `New chat` (chat A), ask a question.
4) `Chats` -> `New chat` (chat B), ask a question.
5) Switch back to chat A: history must still be there.
6) `More` -> `Export` and confirm the shared text includes citations.

iOS:
1) `cd "/Users/shenkairan/Documents/RegShield CA" && npm run ios:staging:up`
2) Go to `Ask` tab.
3) `Chats` -> `New` (chat A), ask a question.
4) `Chats` -> `New` (chat B), ask a question.
5) Switch back to chat A: history must still be there.
6) `…` -> `Export` and confirm the shared text includes citations.

## Rollback / Recovery

- Client-only rollback (no server changes required):
  - Revert the commit(s) implementing multi-chat and rebuild apps.
  - Local storage keys may remain on devices; they are local-only and can be cleared by uninstalling the app or clearing site storage.

## Notes

- Model context is intentionally truncated per request (server limits). The UI retains full local history for the user, but the model only sees the most recent slice.
