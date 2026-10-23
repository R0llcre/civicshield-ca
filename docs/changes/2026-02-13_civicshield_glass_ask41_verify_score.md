# Change Log

Date: 2026-02-13

Owner: Codex (branch: `codex/civicshield-glass-ask41`, commit: `3e4289f`)

Topic: Android chrome polish (CivicShield + no overlap + glass), Ask RAG runs on `gpt-4.1`, and Verify similarity score fix.

## Why

- Android UI issue: the floating top pill could overlap scroll content (it looked "squeezed" when the list header scrolled under it).
- Branding: "RegShield CA" was not a great product name for the public app; we want a shorter, cleaner name.
- Ask (RAG): `gpt-5-mini` required OpenAI organization verification for this key/org, blocking staging `/api/ask`. We needed a model that works now.
- Verify scoring: DB relevance floor made many results share identical scores (e.g., all 65), producing misleading match percentages in UI.

## What Changed

- Android app name + top bar:
  - `<repo-root>/apps/android/app/src/main/res/values/strings.xml`
    - `app_name` -> `CivicShield`
  - `<repo-root>/apps/android/app/src/main/java/org/regshield/ca/ui/components/AppChrome.kt`
    - Top pill reads `R.string.app_name` (no hardcoded string)
    - Glass-like "pill" surface for top and bottom chrome
    - No overlap fix: screens now treat chrome top inset as viewport padding, not scrollable content padding
  - `<repo-root>/apps/android/app/src/main/java/org/regshield/ca/ui/screens/verify/VerifyScreen.kt`
  - `<repo-root>/apps/android/app/src/main/java/org/regshield/ca/ui/screens/help/HelpScreen.kt`
  - `<repo-root>/apps/android/app/src/main/java/org/regshield/ca/ui/screens/shield/ShieldScreen.kt`
  - `<repo-root>/apps/android/app/src/main/java/org/regshield/ca/ui/screens/ask/AskScreen.kt`
  - `<repo-root>/apps/android/app/src/main/java/org/regshield/ca/vault/VaultScreen.kt`

- Shield summary header uses app name:
  - `<repo-root>/apps/android/app/src/main/java/org/regshield/ca/ui/screens/shield/ShieldSummary.kt`
  - `<repo-root>/apps/android/app/src/main/java/org/regshield/ca/ui/screens/shield/ShieldScreen.kt`
  - `<repo-root>/apps/android/app/src/test/java/org/regshield/ca/ui/screens/shield/ShieldSummaryTest.kt`

- Ask RAG model default -> `gpt-4.1` and model-specific payload:
  - `<repo-root>/apps/web/src/server/env.ts`
  - `<repo-root>/apps/web/src/server/openai.ts`
    - Only GPT-5 family requests include `reasoning` and `text.verbosity`
    - Non-GPT-5 models (e.g., `gpt-4.1`) omit those fields
  - `<repo-root>/apps/web/src/app/api/ask/route.test.ts`
  - Azure helper:
    - `<repo-root>/scripts/azure_staging_enable_ask_rag.sh` default model -> `gpt-4.1`

- Verify score fix (DB): return similarity percentage as `score`, keep ordering via a separate `rank`.
  - `<repo-root>/db/migrations/0006_search_ra_entries_similarity_score.sql`
  - `<repo-root>/supabase/migrations/0006_search_ra_entries_similarity_score.sql`
  - `<repo-root>/.github/workflows/db-migrate-azure-staging.yml` applies 0006 on staging

## How To Verify (Commands + Results)

Web checks:

```bash
cd "<repo-root>"
npm -w apps/web run lint
npm -w apps/web run typecheck
npm -w apps/web run test
```

Result: all passed.

Android checks:

```bash
cd "<repo-root>"
JAVA_HOME=$(/usr/libexec/java_home -v 17) ANDROID_SDK_ROOT="$HOME/Library/Android/sdk" \
./gradlew :app:test :app:assembleStagingDebug
```

Result: build + unit tests passed.

Azure staging deploy + Ask enable:

```bash
cd "<repo-root>"
bash scripts/azure_staging_deploy_web.sh
bash scripts/azure_staging_enable_ask_rag.sh
node scripts/smoke_api.mjs --base-url "https://ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io" --max-ra-age-hours 48
```

Result:
- `/api/meta/health` ok
- `/api/ask` returns structured JSON
- smoke: health/verify/directory ok

DB migrations on staging (private Postgres):
- GitHub Actions: `DB Migrate - Azure Staging` (workflow_dispatch)
- Run: `https://github.com/R0llcre/regshield-ca/actions/runs/22001886052`

Verify score sanity (staging):

```bash
curl -fsS "https://ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io/api/verify/ra?name=samantha&state=CA" \
| jq '.results[:8] | map({name,score,matched_on,entry_type})'
```

Expected: `score` is a realistic 0..100 similarity percentage (not all identical values).

## How To Demo (Step-by-step)

Android (staging):

```bash
cd "<repo-root>"
npm run android:staging:up
```

In the emulator:
1. Verify: search `samantha` (CA). Confirm:
   - header/results never slide under the top pill
   - match % varies across results (not all equal)
2. Shield: toggle a couple red flags, then Copy summary (header uses CivicShield).
3. Ask: ask "How do I verify an accredited representative in California?"
   - you should see citations + suggested actions
4. Help: run a directory search (seeded results appear).

Web (staging):
- Open `https://ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io/en/ask`

## Rollback / Recovery

Web (Azure Container Apps):

```bash
az containerapp revision list -g rg-regshieldca-stg-westus2 -n ca-regshieldca-web-stg -o table
az containerapp revision activate -g rg-regshieldca-stg-westus2 -n ca-regshieldca-web-stg --revision <REVISION_NAME>
```

Ask (disable quickly):
- Remove `OPENAI_API_KEY` / `OPENAI_VECTOR_STORE_ID` env vars (or set an empty value) on the Container App and restart.

DB (staging):
- Re-run the migrate workflow; staging can be rebuilt if needed.

## Notes

- Android glass is "glass-like" (no real blur) for stability/perf.
- `OPENAI_MODEL` default is now `gpt-4.1` to avoid org verification gating for staging.

