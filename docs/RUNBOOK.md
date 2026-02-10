# Runbook (MVP)

This runbook focuses on getting a **staging** environment up for internal/partner testing, then promoting to **production**.

## 1. Supabase (DB + Auth)

1. Create a Supabase project (staging first).
2. In the Supabase SQL editor, apply migrations in order:
   - `supabase/migrations/0001_init.sql`
   - `supabase/migrations/0002_aid_org_provenance.sql`
   - `supabase/migrations/0003_search_ra_entries.sql`
   - `supabase/migrations/0004_partner_hardening.sql`
3. Optional: load demo directory data:
   - `supabase/seed.sql`

### Auth settings (recommended for pilot)

In Supabase Auth settings:
- Disable public signups (partners should be provisioned via admin/service role).
- Ensure redirect URLs include:
  - `https://<your-domain>/partner`
  - `http://localhost:3000/partner`

## 2. Vercel (Web)

1. Create a Vercel project pointing at `apps/web`.
2. Set environment variables (staging first):
   - `NEXT_PUBLIC_SUPABASE_URL`
   - `NEXT_PUBLIC_SUPABASE_ANON_KEY`
   - `NEXT_PUBLIC_SENTRY_DSN` (optional)
3. Deploy.

Health check endpoint:
- `GET /api/meta/health`

## 3. GitHub Actions ETL

Workflow: `.github/workflows/etl-ra-roster.yml`

Secrets required:
- `DATABASE_URL` (staging DB URL recommended)

Schedule:
- Runs daily at **06:00 America/Los_Angeles** (DST-safe via dual UTC schedule + in-job guard).

Artifacts:
- `etl_summary` is uploaded for every run that executes ETL (`etl_artifacts/summary.json`).
- On failure, `etl_failure_artifacts` includes DOJ PDFs + parsed `entries.jsonl` (when available).

Alerting (Email via GitHub notifications):
- On ETL failure, the workflow creates or updates a GitHub Issue titled:
  - `ETL Failure: DOJ R&A Roster`
- Ensure at least one maintainer is subscribed to repository issue notifications.

## 4. Partner Provisioning (No Admin UI)

The partner portal requires:
- A Supabase Auth user
- A row in `public.aid_org_membership` binding `user_id` to `org_id`

Provision with the script:

```bash
# env (DO NOT commit)
export SUPABASE_URL="https://<project>.supabase.co"
export SUPABASE_SERVICE_ROLE_KEY="..."

# run
npm run partner:provision -- --email partner@example.org --org-id <aid_org_uuid> --role editor
```

Notes:
- The script attempts to **invite** the user by email; if the user already exists, it will re-use the existing user id.
- Partners log in via magic link on `/partner`.

## 5. Incident Playbooks

### ETL Failure

1. Open the GitHub Actions run logs.
2. Download `etl_summary` and (if present) `etl_failure_artifacts`.
3. Reproduce locally:

```bash
python3 -m venv .venv && source .venv/bin/activate
python3 -m pip install -r etl/requirements.txt
python3 etl/ra_roster_etl.py --dry-run --artifacts-dir etl_artifacts --output-summary etl_artifacts/summary.json
```

4. If parsing broke due to DOJ PDF format change:
   - update `etl/ra_parser.py`
   - add/adjust tests in `etl/tests/`
   - rerun ETL

### Partner Update Issues (403 / permission denied)

1. Confirm the user exists in Supabase Auth.
2. Confirm `aid_org_membership` has the correct `user_id` + `org_id`.
3. Confirm `0004_partner_hardening.sql` has been applied (column-level update grants).

