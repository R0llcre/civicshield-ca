# 2026-02-23 Stability/Perf Phase 1 Submission

## Why
- Android staging gate previously had intermittent incremental build failures (`classes.jar already contains ... R$anim.class`).
- Local/CI flow needed a stable Android gate path with watchdog protection and artifact logs.
- iOS runtime/build path needed to stay on the known-stable route (no TCA macro re-introduction, no overlay regressions).
- Repository had high risk of accidental commits from local worktrees and non-product artifacts.

## What changed
- Hardened Android retry logic for duplicate build artifacts:
  - `scripts/android_staging_run.sh`
  - `scripts/android_dev_run.sh`
- Added Android staging stability gate with pre-clean + watchdog:
  - `scripts/android_stability_gate.sh`
  - `scripts/android_gradle_watchdog.py`
- Added nightly full Android matrix gate script/workflow:
  - `scripts/android_nightly_full_gate.sh`
  - `.github/workflows/android-nightly-matrix.yml`
- Updated CI Android job to use stability gate and upload logs:
  - `.github/workflows/ci.yml`
- Updated staging install flow to force single-worker Gradle during install step:
  - `scripts/android_staging_up.sh`
- Added/updated npm scripts for stability/perf workflows:
  - `package.json`
- Prevent accidental local worktree commits:
  - `.gitignore` (`.claude/worktrees/`)
- Updated execution checklist to the new repo path and stability-first runbook:
  - `docs/ENGINEERING_CHECKLIST.md`

## How to verify
Commands executed from `/Users/shenkairan/Documents/Spring26/RegShield CA`:

1. Web
- `npm -w apps/web run lint` ✅
- `npm -w apps/web run typecheck` ✅
- `npm -w apps/web run test` ✅ (5 files, 16 tests passed)

2. iOS
- `cd apps/ios/CivicShield && xcodegen generate` ✅
- `xcodebuild build -project CivicShield.xcodeproj -scheme CivicShield-Staging -destination "platform=iOS Simulator,name=iPhone 15 Pro Max,OS=17.5"` ✅
- `xcodebuild test -project CivicShield.xcodeproj -scheme CivicShield-Staging -destination "platform=iOS Simulator,name=iPhone 15 Pro Max,OS=17.5"` ✅ (4 tests passed)

3. Android
- `npm run android:staging:gate` ✅ (run #1 passed)
- `npm run android:staging:gate` ✅ (run #2 passed)
- `npm run android:staging:up` ✅ (install + launch succeeded)

## How to demo
1. Android staging launch:
- `npm run android:staging:up`
- Validate Verify/Shield/Ask/Help/Vault main paths.
2. iOS staging launch:
- `npm run ios:staging:up`
- Validate Ask tab switch responsiveness and main tab flows.

## Rollback
- Revert the merge/commit range on `codex/stability-perf-phase1`:
  - `git revert <sha>` (or revert the PR once merged)
- If CI change causes issues, revert:
  - `.github/workflows/ci.yml`
  - `.github/workflows/android-nightly-matrix.yml`
