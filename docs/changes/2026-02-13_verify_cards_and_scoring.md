# Change Log

Date: 2026-02-13

Owner: Codex (repo agent) + shenkairan

Topic: Android Verify polish (full-width cards) + roster relevance scoring (0..100)

## Why

- Android Verify results looked uneven because cards were wrapping content width, creating inconsistent box sizes.
- The roster `score` returned by `search_ra_entries()` was previously a raw rank value (similarity + boosts), which could be `> 1`. Android interpreted `score > 1` as a percent, leading to incorrect match percentages.
- We need a user-facing, explainable `score` in the `0..100` range.

## What Changed

Android UI:
- `/Users/shenkairan/Documents/RegShield CA/apps/android/app/src/main/java/org/regshield/ca/ui/components/CivicComponents.kt`
  - `CivicCard()` now defaults to `fillMaxWidth()` for consistent scanning.
- `/Users/shenkairan/Documents/RegShield CA/apps/android/app/src/main/java/org/regshield/ca/ui/screens/verify/VerifyScreen.kt`
  - Representative rows no longer show `• US` when the roster lacks state/city (reduces confusion with state filter).
- `/Users/shenkairan/Documents/RegShield CA/apps/android/app/src/main/res/values/strings.xml`
  - Results hint changed to "Sorted by relevance".

DB scoring (portable migrations):
- `/Users/shenkairan/Documents/RegShield CA/db/migrations/0005_search_ra_entries_relevance_score.sql`
  - Replaces `score` with relevance percent `0..100` using `pg_trgm` similarity plus explainable exact/prefix/contains floors.
- `/Users/shenkairan/Documents/RegShield CA/supabase/migrations/0005_search_ra_entries_relevance_score.sql`
  - Same function update for Supabase environments.
- `/Users/shenkairan/Documents/RegShield CA/.github/workflows/db-migrate-azure-staging.yml`
  - Applies the new 0005 migration on Azure staging.

Docs:
- `/Users/shenkairan/Documents/RegShield CA/docs/ENGINEERING_CHECKLIST.md`
  - Added a reminder step for Azure staging DB migrations when `db/migrations/*` changes.

## How To Verify (Commands + Results)

Web (local):

```bash
cd "/Users/shenkairan/Documents/RegShield CA"
npm -w apps/web run lint
npm -w apps/web run typecheck
npm -w apps/web run test
```

Android (local):

```bash
cd "/Users/shenkairan/Documents/RegShield CA"
JAVA_HOME=$(/usr/libexec/java_home -v 17) ANDROID_SDK_ROOT="$HOME/Library/Android/sdk" \
./gradlew :app:test :app:assembleStagingDebug
```

Azure staging DB migration:
- GitHub Actions: run `DB Migrate - Azure Staging` (workflow_dispatch)

Then confirm score ranges:

```bash
curl -fsS "https://ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io/api/verify/ra?name=samantha&state=CA" \
| jq '.results[:5] | map({name,entry_type,score,matched_on,organization_name,city,state})'
```

Expected:
- `score` (when present) is always within `0..100`.

## How To Demo (Step-by-step)

1) Install/launch Android staging build:

```bash
cd "/Users/shenkairan/Documents/RegShield CA"
npm run android:staging:up
```

2) In the app:
- Open `Verify`
- Search `samantha` (CA)
- Confirm:
  - Result cards are full width and aligned.
  - Representative items do not show `• US`.
  - MatchChip percent looks reasonable (not `1%` due to `score > 1`).

## Rollback / Recovery

Android: revert the commit(s) touching `CivicComponents.kt` / `VerifyScreen.kt`.

DB: re-apply the previous function definition (from migration 0003), or roll back the staging DB to a prior point-in-time if needed.

