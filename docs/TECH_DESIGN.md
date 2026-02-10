# Technical Design (MVP)

## Architecture

- `apps/web`: Next.js App Router + TypeScript + Tailwind v4 + next-intl + Radix UI
- `etl`: Python (requests + pdfplumber) to download DOJ PDFs, parse, and upsert into Postgres
- `supabase`: SQL migration enabling `pg_trgm` and RLS policies

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
