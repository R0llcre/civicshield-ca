# Runbook (MVP)

## Deploy (Vercel + Supabase)

1. Create Supabase project
2. Run `supabase/migrations/0001_init.sql` in Supabase SQL editor
3. (Optional) Run `supabase/seed.sql` for demo org data
4. Create Vercel project for `apps/web`
5. Set env vars in Vercel:
   - `NEXT_PUBLIC_SUPABASE_URL`
   - `NEXT_PUBLIC_SUPABASE_ANON_KEY`

## ETL (GitHub Actions)

Workflow: `.github/workflows/etl-ra-roster.yml`

Secrets required:
- `DATABASE_URL`

## Incident: ETL Failure

1. Open workflow logs
2. Check if DOJ PDF format changed
3. Use `--dry-run --output-json out.jsonl` locally to inspect parsing
4. If parsing is broken:
   - update parser in `etl/ra_parser.py`
   - add/adjust tests in `etl/tests/`
   - rerun ETL

