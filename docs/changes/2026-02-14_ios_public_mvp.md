# 2026-02-14 — iOS Public MVP (CivicShield) + Evidence Vault UI + Simulator Up Script

Date: 2026-02-14

Owner: shenkairan

Topic: iOS public app (Verify/Shield/Ask/Help/Vault) runnable on iOS Simulator with one command; vault zip interoperability tests.

## Why

- We needed an iOS public client that can be run and demoed on a simulator (staging), matching Android/Web flows.
- Evidence Vault must remain local-only and encrypted, and must interoperate with existing Android/Web vault zip format.
- We also needed repo hygiene: keep generated Xcode artifacts out of git while making the project easy to start.

## What Changed

- Added iOS app sources (XcodeGen-based project):
  - `<repo-root>/apps/ios/CivicShield/`
- Fixed XcodeGen + SPM integration and Swift 6 build behavior:
  - `<repo-root>/apps/ios/CivicShield/project.yml`
- Fixed an iOS compile error in Help:
  - `<repo-root>/apps/ios/CivicShield/Sources/Features/Help/HelpView.swift`
- Fixed a SwiftUI naming conflict in Verify:
  - `<repo-root>/apps/ios/CivicShield/Sources/Features/Verify/VerifyView.swift`
- Added Evidence Vault UI (SwiftUI) wired to existing vault crypto/zip/store:
  - `<repo-root>/apps/ios/CivicShield/Sources/Vault/VaultView.swift`
- Added missing imports for vault code:
  - `<repo-root>/apps/ios/CivicShield/Sources/Vault/VaultStore.swift`
  - `<repo-root>/apps/ios/CivicShield/Sources/Vault/VaultZip.swift`
- Fixed Info.plist required keys for simulator install/test:
  - `<repo-root>/apps/ios/CivicShield/Resources/Info-Dev.plist`
  - `<repo-root>/apps/ios/CivicShield/Resources/Info-Staging.plist`
  - `<repo-root>/apps/ios/CivicShield/Resources/Info-Prod.plist`
- Swift 6 concurrency: marked API client as safe to send across tasks:
  - `<repo-root>/apps/ios/CivicShield/Sources/Networking/ApiClient.swift`
- Ask model types: removed unused `Equatable` conformance that broke synthesis:
  - `<repo-root>/apps/ios/CivicShield/Sources/Networking/Models.swift`
- Added iOS unit tests + cross-platform vault fixture:
  - `<repo-root>/apps/ios/CivicShield/Tests/VaultCryptoTests.swift`
  - `<repo-root>/apps/ios/CivicShield/Tests/VaultZipTests.swift`
  - `<repo-root>/apps/ios/CivicShield/Tests/Resources/vault_web_fixture.zip`
- Added one-command iOS simulator startup script + npm entry:
  - `<repo-root>/scripts/ios_staging_up.sh`
  - `<repo-root>/package.json`
- Repo hygiene/docs:
  - `<repo-root>/.gitignore` (ignore Xcode generated artifacts)
  - `<repo-root>/docs/ENGINEERING_CHECKLIST.md` (iOS verification section)
  - `<repo-root>/apps/ios/CivicShield/README.md`
  - `<repo-root>/README.md`

Behavior changes:

- iOS: Evidence Vault is now accessible from Shield and supports create/unlock/import/export/add/delete/lock flows.
- No backend API contract changes.

## How To Verify (Commands + Results)

1) Web (unchanged but still required by checklist):

```bash
cd "<repo-root>"
npm -w apps/web run lint
npm -w apps/web run typecheck
npm -w apps/web run test
```

Results: PASS (2026-02-14)

2) iOS build + unit tests:

```bash
cd "<repo-root>/apps/ios/CivicShield"
xcodegen generate
xcodebuild test \
  -project CivicShield.xcodeproj \
  -scheme CivicShield-Staging \
  -destination "platform=iOS Simulator,name=iPhone 15 Pro Max,OS=17.5"
```

Results: PASS (2026-02-14)

3) API smoke (staging):

```bash
STG="https://ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io"
curl -fsS "$STG/api/meta/health" | jq
curl -fsS -X POST "$STG/api/ask" -H "content-type: application/json" \
  -d '{"locale":"en","messages":[{"role":"user","content":"How do I verify an accredited representative in California?"}]}' | jq
```

Results: PASS (2026-02-14) — `/api/meta/health` ok, `/api/ask` returned answer + citations/actions.

## How To Demo (Step-by-step)

iOS (staging, one command):

```bash
cd "<repo-root>"
npm run ios:staging:up
```

In the iOS Simulator:

1) Verify: search `Diaz` -> open `Open official source`.
2) Help: confirm featured list loads; try filters and `Find organizations`.
3) Ask: ask a question; confirm markdown renders; citations open (allowlist).
4) Shield: toggle flags/steps; copy/share summary; open Evidence Vault.
5) Evidence Vault:
   - Create -> Add files -> Export zip
   - Delete vault -> Import zip -> Unlock -> items restored
   - Wrong passphrase must show only generic failure text.

## Rollback / Recovery

- iOS: revert this commit range and rerun `npm run ios:staging:up`.
- No Azure/DB rollback required (client-only change).

## Notes

- We intentionally do not commit generated `*.xcodeproj` output; `xcodegen generate` is required.
- Vault is local-only; the app must not upload evidence or log file contents.
