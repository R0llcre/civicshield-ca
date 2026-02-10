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

## Environment Variables

Create a `.env.local` (or `.env`) at repo root:

```bash
# Web (public)
NEXT_PUBLIC_SUPABASE_URL=
NEXT_PUBLIC_SUPABASE_ANON_KEY=

# ETL (server)
DATABASE_URL=
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

Run the migration in `supabase/migrations/0001_init.sql` in your Supabase project's SQL editor.

Optional demo seed:

- `supabase/seed.sql`

Before any real pilot, replace demo directory data with verified organizations.

