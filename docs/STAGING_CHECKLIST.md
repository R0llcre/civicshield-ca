# Staging Checklist (Pilot Readiness)

This checklist is the minimal set of steps to get a **staging** environment ready for internal + partner testing.

## 1) Supabase: `regshield-ca-staging`

- Create Supabase project: `regshield-ca-staging`
- Apply migrations in order:
  - `supabase/migrations/0001_init.sql`
  - `supabase/migrations/0002_aid_org_provenance.sql`
  - `supabase/migrations/0003_search_ra_entries.sql`
  - `supabase/migrations/0004_partner_hardening.sql`
- (Optional, pre-pilot demo only) Load seed:
  - `supabase/seed.sql`

Auth settings:
- Disable public signups
- Add redirect URLs:
  - `http://localhost:3000/partner`
  - `https://<vercel-staging-host>/partner`

## 2) Vercel: `regshield-ca-staging`

- Create Vercel project pointing at `apps/web`
- Configure env vars:
  - `NEXT_PUBLIC_SUPABASE_URL`
  - `NEXT_PUBLIC_SUPABASE_ANON_KEY`
  - `NEXT_PUBLIC_SENTRY_DSN` (optional)
- Deploy

Smoke checks:
- `GET https://<vercel-staging-host>/api/meta/health`
- Verify `/verify`, `/shield`, `/help` render

## 3) GitHub Actions ETL (staging DB)

- Add repo secret:
  - `DATABASE_URL` (staging Postgres URL)
- Run workflow manually:
  - `.github/workflows/etl-ra-roster.yml`
- Confirm artifacts:
  - `etl_summary` always present for runs that execute ETL
  - `etl_failure_artifacts` present on failure
- Confirm alerting:
  - failure creates/updates a GitHub Issue `ETL Failure: DOJ R&A Roster`
  - maintainers receive email via GitHub notifications

## 4) Android (Internal APK)

Build `stagingDebug` APK:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
ANDROID_SDK_ROOT="$HOME/Library/Android/sdk" \
./gradlew :app:assembleStagingDebug
```

Before sharing to pilot users (recommended):
- Build with a real staging base URL (no file edits):

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
ANDROID_SDK_ROOT="$HOME/Library/Android/sdk" \
./gradlew :app:assembleStagingDebug -PREGSHIELD_STAGING_BASE_URL="https://<vercel-staging-host>"
```

Optional (upgradeable internal release):
- Configure signing secrets in GitHub
- Run workflow: `.github/workflows/android-internal-release.yml`

APK output:
- `apps/android/app/build/outputs/apk/staging/debug/app-staging-debug.apk`

## 5) API Smoke Script (optional but recommended)

Run:

```bash
node scripts/smoke_api.mjs --base-url "https://<vercel-staging-host>"
```
