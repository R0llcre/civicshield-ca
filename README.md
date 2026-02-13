# RegShield CA

RegShield CA is a California-focused safety layer for immigration legal help:

- **Verify**: check official DOJ EOIR Recognition & Accreditation (R&A) rosters (and link out to CA State Bar / CA SOS).
- **Shield**: scam red flags + a **local, encrypted** evidence vault (no uploads).
- **Help**: lightweight triage to find organizations by county/language/service + intake status.

This repo is a small monorepo:

- `apps/web` Next.js (App Router) web app
- `etl` Python ETL to sync DOJ roster PDFs into Postgres/Supabase
- `supabase` SQL migrations + demo seed data
- `docs` PRD, tech design, privacy/threat model, data sources, runbook

## Quickstart (Web)

```bash
npm install
npm run dev
```

Then open `http://localhost:3000`.

Staging readiness check (after provisioning Supabase + Vercel):

```bash
STAGING_BASE_URL="https://<vercel-staging-host>" npm run preflight:staging
```

## Quickstart (Android)

Prereqs:

- Android Studio + Android SDK
- JDK 17 (Gradle/AGP requires 17+)

Build a staging debug APK:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
ANDROID_SDK_ROOT="$HOME/Library/Android/sdk" \
./gradlew :app:assembleStagingDebug
```

Override the staging API base URL at build time (recommended, avoids editing files):

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
ANDROID_SDK_ROOT="$HOME/Library/Android/sdk" \
./gradlew :app:assembleStagingDebug -PREGSHIELD_STAGING_BASE_URL="https://<vercel-staging-host>"
```

APK output:

- `apps/android/app/build/outputs/apk/staging/debug/app-staging-debug.apk`

Local dev against the web server (Android emulator):

1. Run `npm run dev` (serves `http://localhost:3000`)
2. Build/install `devDebug` (API base URL is `http://10.0.2.2:3000`)

Convenience commands:

```bash
npm run android:dev:assemble
npm run android:dev:install
```

If kapt duplicate-file errors appear (`*Factory 2.java`), use:

```bash
npm run android:dev:clean-assemble
```

## Environment Variables

Create a `.env.local` (or `.env`) at repo root:

```bash
# Web (public)
NEXT_PUBLIC_SUPABASE_URL=
NEXT_PUBLIC_SUPABASE_ANON_KEY=

# Optional: client-side error monitoring
NEXT_PUBLIC_SENTRY_DSN=

# ETL (server)
DATABASE_URL=

# Ops scripts (server/admin; keep secret)
SUPABASE_SERVICE_ROLE_KEY=
```

Without Supabase env vars, the web app runs in **demo mode** using sample data.

## ETL

Install deps:

```bash
python3 -m venv .venv
source .venv/bin/activate
python3 -m pip install -r etl/requirements.txt
```

Dry-run parse (no DB writes):

```bash
python3 etl/ra_roster_etl.py --dry-run
```

Run with DB writes:

```bash
DATABASE_URL="postgresql://..." python3 etl/ra_roster_etl.py
```

## Supabase

Run the migrations in `supabase/migrations/` (in order) in your Supabase project's SQL editor:

- `0001_init.sql`
- `0002_aid_org_provenance.sql`
- `0003_search_ra_entries.sql`
- `0004_partner_hardening.sql`

Optional demo seed:

- `supabase/seed.sql`

Before any real pilot, replace demo directory data with verified organizations.
