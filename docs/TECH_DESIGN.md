# Technical Design (MVP)

## Architecture

- `apps/web`: Next.js App Router + TypeScript + Tailwind v4 + next-intl + Radix UI
- `etl`: Python (requests + pdfplumber) to download DOJ PDFs, parse, and upsert into Postgres
- `supabase`: SQL migration enabling `pg_trgm` and RLS policies
- `db/migrations`: **portable Postgres migrations** (for Azure Database for PostgreSQL, no Supabase auth/RLS)

## Data Sets

We ingest two DOJ PDFs:

1. **Organizations by State/City** (`dataset=orgs_by_state_city`): provides organization contact info.
2. **Accredited Representatives roster** (`dataset=reps_roster`): provides representatives and their recognized org.

## Tables

See `supabase/migrations/0001_init.sql` for canonical schema:

- `ra_import_run`
- `ra_entry`
- `aid_org`
- `aid_org_membership`

For **Azure Postgres** (Full Azure), apply the portable migrations instead:

- `db/migrations/0001_init.sql`
- `db/migrations/0002_aid_org_provenance.sql`
- `db/migrations/0003_search_ra_entries.sql`
- `db/migrations/0004_partner_security.sql`

## APIs

- `GET /api/verify/ra?name=...&state=CA`
  - Searches representatives nationally + organizations filtered by state.
  - Returns `results[]` and `data_freshness` (best effort).
- `POST /api/verify/ra`
  - Preferred by the UI to avoid putting names in URL query strings.
- `GET /api/directory/search?county=...&lang=...&service=...`
  - Filters `aid_org` using array containment.
- `POST /api/partner/org/:orgId`
  - Requires `Authorization: Bearer <supabase access token>`.
  - RLS in Supabase enforces org membership.
- `GET /api/meta/health`
  - Basic readiness endpoint (Supabase configured + latest roster sync timestamps).

## Data Access Modes (Supabase vs Azure Postgres)

The Web/API service supports two data sources:

1. **Supabase** (default MVP)
   - Configure: `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`
   - API routes query Supabase directly (and rely on Supabase RLS for partner writes).
2. **Direct Postgres** (Full Azure)
   - Configure: `DATABASE_URL` (server-side only; should include `?sslmode=require` for Azure PG)
   - API routes will prefer Postgres over Supabase when `DATABASE_URL` is set.
   - Partner write hardening is implemented via DB functions in `db/migrations/0004_partner_security.sql`.

## ETL

`etl/ra_roster_etl.py`:

- Downloads PDFs
- Computes SHA-256
- Skips DB writes if hash unchanged (per dataset)
- Parses and upserts into `ra_entry`
- Writes `ra_import_run` rows for success/failure tracking

## Search

- `pg_trgm` indexes exist on `ra_entry.name` and `ra_entry.organization_name`.
- Primary search path uses `public.search_ra_entries(q, state, lim)` (SQL/RPC) to rank results via `pg_trgm` similarity.
- API falls back to `ILIKE` if the RPC is missing (early setups).
