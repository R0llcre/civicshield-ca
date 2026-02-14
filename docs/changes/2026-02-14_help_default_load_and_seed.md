# Change Log

Date: 2026-02-14

Owner: Codex

Topic: Help tab is no longer "empty" (auto-load + official directory links + richer demo seed)

## Why

- Help looked empty because results only loaded after tapping `Find organizations`, and staging only had 2 demo orgs.
- For a product-like experience, Help should show useful content immediately and still provide official next-step links when directory data is sparse.

## What Changed

- Android auto-load on first open + better empty-state gating:
  - `/Users/shenkairan/Documents/RegShield CA/apps/android/app/src/main/java/org/regshield/ca/ui/screens/help/HelpViewModel.kt`
  - `/Users/shenkairan/Documents/RegShield CA/apps/android/app/src/main/java/org/regshield/ca/ui/screens/help/HelpScreen.kt`
- Android "Official directories" card (LawHelpCA / Immigration Advocates / DOJ EOIR R&A page):
  - `/Users/shenkairan/Documents/RegShield CA/apps/android/app/src/main/res/values/strings.xml`
- Web Help auto-load on first page load:
  - `/Users/shenkairan/Documents/RegShield CA/apps/web/src/features/help/HelpClient.tsx`
- Expand demo directory data to 12+ entries:
  - Azure staging seed: `/Users/shenkairan/Documents/RegShield CA/db/seed/staging_demo.sql`
  - No-DB fallback sample: `/Users/shenkairan/Documents/RegShield CA/apps/web/src/data/aid_org_sample.ts`
- Checklist update: seed changes require running the staging DB migrate workflow.
  - `/Users/shenkairan/Documents/RegShield CA/docs/ENGINEERING_CHECKLIST.md`

Behavior changes:
- Android Help now performs an initial "featured" directory search (no filters) automatically the first time the tab opens.
- Android "No organizations match" only shows after at least one search has completed.
- Web Help now auto-runs the search on mount (no filters).

## How To Verify (Commands + Results)

Web:

```bash
cd "/Users/shenkairan/Documents/RegShield CA"
npm -w apps/web run lint
npm -w apps/web run typecheck
npm -w apps/web run test
```

Result: PASS

Android:

```bash
cd "/Users/shenkairan/Documents/RegShield CA"
JAVA_HOME=$(/usr/libexec/java_home -v 17) ANDROID_SDK_ROOT="$HOME/Library/Android/sdk" \
./gradlew :app:test :app:assembleStagingDebug
```

Result: PASS

Azure staging seed (required after updating `db/seed/staging_demo.sql`):

1) Run GitHub Actions workflow:
- `DB Migrate - Azure Staging` (workflow_dispatch) on this branch/ref
  - Run: `22009495025` (success)

2) Smoke check count:

```bash
STG="https://ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io"
curl -fsS "$STG/api/directory/search" | jq '{count:(.results|length)}'
```

Result: `{"count":12}`

## How To Demo (Step-by-step)

Android (staging):

```bash
cd "/Users/shenkairan/Documents/RegShield CA"
npm run android:staging:up
```

1) Open `Help` tab.
2) Expected:
   - A list of organizations appears without tapping `Find organizations`.
   - "Official directories" card is present and its links open the browser.
3) Change filters (County/Language/Service), tap `Find organizations`, confirm list changes.

Web (staging):
- Open `/help` on the staging host.
- Confirm results auto-load without clicking Search.

## Rollback / Recovery

- Code rollback: revert the commit(s) for this change.
- Staging DB seed rollback:
  - Re-run `DB Migrate - Azure Staging` with a previous commit that contains the prior `db/seed/staging_demo.sql`, or edit the seed to reduce the dataset and re-run the workflow.

## Notes

- Demo organizations are clearly labeled `(Staging Demo)` and use `example.org` URLs to avoid confusion with real providers.
