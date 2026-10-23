# 2026-02-14 — iOS Device Code Signing Enabled (Fix Unsigned .app)

Date: 2026-02-14

Owner: shenkairan / Codex

Topic: iOS device install fails with “The executable is not codesigned”

## Why

Running CivicShield on a real iPhone failed because the app binary was built **without a code signature**. The project generator (`xcodegen` via `project.yml`) previously set `CODE_SIGNING_ALLOWED=NO` globally, so device builds produced an unsigned `.app`, and iOS refused to install it.

## What Changed

- Enabled signing for device builds while keeping simulator builds frictionless:
  - `<repo-root>/apps/ios/CivicShield/project.yml`
    - `CODE_SIGNING_ALLOWED[sdk=iphoneos*] = YES`
    - `CODE_SIGNING_ALLOWED[sdk=iphonesimulator*] = NO`

- Documented how to run on a real iPhone (Signing & Capabilities steps):
  - `<repo-root>/apps/ios/CivicShield/README.md`

## How To Verify (Commands + Results)

Generate project:

```bash
cd "<repo-root>/apps/ios/CivicShield"
xcodegen generate
```

Confirm build settings (expect: simulator `NO`, device `YES`):

```bash
cd "<repo-root>/apps/ios/CivicShield"

# Simulator
xcodebuild -project CivicShield.xcodeproj -scheme CivicShield-Staging -configuration Staging \
  -sdk iphonesimulator -destination "id=242D3B85-1E5C-408F-B5E8-A264EDF321C2" \
  -showBuildSettings | rg "CODE_SIGNING_ALLOWED" | head

# Device (generic)
xcodebuild -project CivicShield.xcodeproj -scheme CivicShield-Staging -configuration Staging \
  -sdk iphoneos -destination "generic/platform=iOS" \
  -showBuildSettings | rg "CODE_SIGNING_ALLOWED" | head
```

Run unit tests (simulator):

```bash
cd "<repo-root>/apps/ios/CivicShield"
xcodebuild test \
  -project CivicShield.xcodeproj \
  -scheme CivicShield-Staging \
  -destination "platform=iOS Simulator,name=iPhone 15 Pro Max,OS=17.5"
```

## How To Demo

Simulator (one command):

```bash
cd "<repo-root>"
npm run ios:staging:up
```

Real iPhone:

1) `xcodegen generate`, open `CivicShield.xcodeproj`
2) Target `CivicShield` -> `Signing & Capabilities`:
   - `Automatically manage signing` = ON
   - Select your Team
3) Connect iPhone, trust the computer, select iPhone as the run destination, Run.

## Rollback / Recovery

- Revert this change and regenerate the project:
  - `git revert <commit>`
  - `xcodegen generate`
- Note: rolling back will restore the behavior where device builds are not signed (and iPhone install will fail).

