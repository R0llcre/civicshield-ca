# Change Log

Date: 2026-02-14

Owner: Codex

Topic: Help auto-load featured results + official directories card + expand demo seed to 12+

## Why

- Help felt "empty" unless the user manually pressed search, and staging only had 2 demo orgs, so filters did not feel like a real product.
- We want Help to be useful immediately on first open, even before a user understands filters.

Tradeoffs:
- We keep the public API contract unchanged (`GET /api/directory/search`), so the default load is implemented client-side by calling the same endpoint without filters.
- Demo data is clearly labeled `(Demo)` / `(Staging Demo)` to avoid confusion with real org data.

## What Changed

- Android: auto-load featured results on first open; only show "no results" after a search completed.
  - `<repo-root>/apps/android/app/src/main/java/org/regshield/ca/ui/screens/help/HelpViewModel.kt`
  - `<repo-root>/apps/android/app/src/main/java/org/regshield/ca/ui/screens/help/HelpScreen.kt`
  - `<repo-root>/apps/android/app/src/main/res/values/strings.xml`
- Android: add an "Official directories" card with trusted external links (LawHelpCA, Immigration Advocates, DOJ/EOIR R&A).
  - `<repo-root>/apps/android/app/src/main/java/org/regshield/ca/ui/screens/help/HelpScreen.kt`
  - `<repo-root>/apps/android/app/src/main/res/values/strings.xml`
- Android: stabilize Compose UI tests by adding scroll-container tags and scrolling to items before asserting inner UI tags.
  - `<repo-root>/apps/android/app/src/main/java/org/regshield/ca/ui/screens/verify/VerifyScreen.kt`
  - `<repo-root>/apps/android/app/src/main/java/org/regshield/ca/ui/screens/help/HelpScreen.kt`
  - `<repo-root>/apps/android/app/src/main/java/org/regshield/ca/ui/screens/shield/ShieldScreen.kt`
  - `<repo-root>/apps/android/app/src/androidTest/java/org/regshield/ca/ui/AppUiFlowsTest.kt`
- Web: auto-load featured results on first open; add a loading state label.
  - `<repo-root>/apps/web/src/features/help/HelpClient.tsx`
  - `<repo-root>/apps/web/messages/en.json`
  - `<repo-root>/apps/web/messages/es.json`
  - `<repo-root>/apps/web/messages/zh-Hans.json`
- Sample/demo orgs: expand to 12+ entries for local (no DB) environments.
  - `<repo-root>/apps/web/src/data/aid_org_sample.ts`
- Azure staging demo seed: expand to 12+ entries (applied by the `DB Migrate - Azure Staging` workflow).
  - `<repo-root>/db/seed/staging_demo.sql`
- Checklist: add a staging `/api/directory/search` smoke command.
  - `<repo-root>/docs/ENGINEERING_CHECKLIST.md`

Behavior changes:
- Android/Web Help now fetches a featured list automatically on first open.
- Staging demo seed is now designed to return 12+ orgs once `DB Migrate - Azure Staging` is re-run.

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

Android UI tests (emulator must be running):

```bash
cd "<repo-root>"
JAVA_HOME=$(/usr/libexec/java_home -v 17) ANDROID_SDK_ROOT="$HOME/Library/Android/sdk" \
./gradlew :app:connectedDevDebugAndroidTest
```

Staging smoke (directory):

```bash
STG="https://ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io"
curl -fsS "$STG/api/directory/search" | jq '{count:(.results|length)}'
```

Expected:
- After running the GitHub Actions workflow `DB Migrate - Azure Staging`, `count` should be >= 12.

## How To Demo (Step-by-step)

1) Apply staging seed (required for the 12+ demo orgs):
- GitHub Actions: run `DB Migrate - Azure Staging` (workflow_dispatch) on this branch/ref.

2) Android demo:

```bash
cd "<repo-root>"
npm run android:staging:up
```

In the emulator:
- Open `Help` tab.
- Expected: a list of organizations appears automatically (no button press needed).
- Change County/Language/Service and press `Find organizations` to see filtered results.
- Open the "Official directories" links; they should open in the external browser.

3) Web demo:
- Open the Help page in staging.
- Expected: results load automatically and show a loading state while fetching.

## Rollback / Recovery

- Code rollback: revert the commit(s) that introduced the auto-load and seed expansion.
- Staging DB rollback:
  - Re-run `DB Migrate - Azure Staging` with the previous `db/seed/staging_demo.sql`, or manually delete demo rows by id and re-seed.

## Notes

- This is still demo data. The next step is a real directory ingestion pipeline (CSV/Sheet import + provenance + periodic re-verification).
