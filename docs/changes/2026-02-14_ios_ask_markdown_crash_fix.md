# 2026-02-14 — iOS Ask Crash Fix (Markdown Rendering on Device)

Date: 2026-02-14

Owner: shenkairan / Codex

Topic: iOS Ask tab crashed/“卡死” on device when rendering markdown

## Why

On real iPhone (iOS 26.x), opening the Ask tab could crash with `SIGABRT` inside Down’s markdown-to-attributed-string path (`NSAttributedString(html: ...)`). This is device/OS-specific and does not reproduce reliably on older simulators.

We need Ask to be robust on device while still supporting full markdown (tables/task lists/code blocks), so we replaced the rendering implementation.

## What Changed

- Replaced iOS markdown rendering from:
  - `UITextView` + `Down.toAttributedString()` (HTML -> `NSAttributedString`), which could crash on device
- To:
  - `WKWebView` rendering (Markdown -> HTML via `Down.toHTML()`), JavaScript disabled, non-persistent data store
  - Link clicks intercepted and allowlisted via `UrlPolicy`
  - External images stripped (`img { display:none }`) to avoid unintended network loads

Files:
- `<repo-root>/apps/ios/CivicShield/Sources/Features/Ask/MarkdownView.swift`

## How To Verify (Commands + Results)

Generate project:

```bash
cd "<repo-root>/apps/ios/CivicShield"
xcodegen generate
```

Run unit tests:

```bash
cd "<repo-root>/apps/ios/CivicShield"
xcodebuild test \
  -project CivicShield.xcodeproj \
  -scheme CivicShield-Staging \
  -destination "platform=iOS Simulator,name=iPhone 15 Pro Max,OS=17.5"
```

Manual device verification:
1) Run `CivicShield-Staging` on iPhone from Xcode
2) Open `Ask` tab
3) Scroll through existing assistant messages (with markdown)
4) Confirm it no longer crashes and links open only for allowlisted domains

## How To Demo

Simulator:

```bash
cd "<repo-root>"
npm run ios:staging:up
```

Device:
1) Open Xcode, pick scheme `CivicShield-Staging`
2) Run on iPhone
3) Go to `Ask`, ask: `How do I verify an accredited representative in California?`

## Rollback / Recovery

- Revert the commit that changes markdown rendering:
  - `git revert <commit>`
  - `xcodegen generate`
- Note: rolling back may reintroduce device crash depending on iOS version.

