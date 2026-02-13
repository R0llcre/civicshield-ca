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

1. One command startup:

```bash
npm run android:dev:up
```

This command will:
- ensure JDK 17
- start/reuse Android emulator (default full-screen strategy: `Pixel_8_Pro_API_34`, fallback `Pixel_8_Pro*API_34`)
- start/reuse local web (`npm run dev`)
- build + install `devDebug`
- launch `org.regshield.ca.dev`
- pin install/launch to the matched target AVD serial

Useful follow-up commands:

```bash
npm run android:dev:status
npm run android:dev:down
```

Fallback build commands:

```bash
npm run android:dev:assemble
npm run android:dev:install
npm run android:dev:clean-assemble
```

Common failures and fixes:
- AVD not found: set `REGSHIELD_ANDROID_AVD=<your_avd_name>` and rerun `android:dev:up`
- JDK not 17: install JDK 17 (Temurin recommended), then rerun
- SDK tools missing (`adb`/`emulator`): install Android SDK Platform-Tools + Emulator
- Emulator boot timeout: retry and inspect log path printed by `android:dev:up`

Recommended device coverage for UI checks:
- Pixel 8 Pro API 34 (full-screen) in portrait + landscape
- One smaller phone profile (for example Pixel 3a) in portrait

Fullscreen Android UI acceptance checklist:
- no oversized blank strip above first hero card
- floating bottom capsule stays above system gesture area
- final card in each screen scrolls above bottom navigation
- bottom navigation icons/labels remain readable at font scale 1.3

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

# AI Q&A (server; keep secret)
OPENAI_API_KEY=
# Optional: override model (default gpt-5-mini)
OPENAI_MODEL=
# Ask RAG (required for /api/ask)
OPENAI_VECTOR_STORE_ID=
# Optional: authoritative web search fallback (default true, cache-only)
OPENAI_ENABLE_WEB_SEARCH=true
OPENAI_WEB_SEARCH_EXTERNAL=false
# Optional: strict mode (default true: refuse if no authoritative citations)
OPENAI_RAG_STRICT=true
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
