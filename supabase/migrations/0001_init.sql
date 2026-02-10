-- RegShield CA - Supabase schema (MVP)
-- Notes:
-- - We enable `pg_trgm` for fuzzy-ish search and `pgcrypto` for UUID helpers.
-- - RLS is enabled where required by the partner portal.

create extension if not exists "pgcrypto";
create extension if not exists "pg_trgm";

-- ---------------------------------------------------------------------------
-- R&A roster import runs
-- ---------------------------------------------------------------------------

create table if not exists public.ra_import_run (
  id uuid primary key default gen_random_uuid(),
  dataset text not null, -- e.g. 'orgs_by_state_city', 'reps_roster'
  started_at timestamptz not null default now(),
  ended_at timestamptz,
  source_url text not null,
  source_sha256 text not null,
  status text not null check (status in ('running', 'success', 'failed')),
  row_count integer,
  error_summary text
);

create index if not exists ra_import_run_dataset_ended_at_idx
  on public.ra_import_run (dataset, ended_at desc);

-- ---------------------------------------------------------------------------
-- R&A roster entries (organizations + accredited representatives)
-- ---------------------------------------------------------------------------

create table if not exists public.ra_entry (
  id uuid primary key default gen_random_uuid(),
  dataset text not null,
  entry_type text not null check (entry_type in ('organization', 'representative')),
  name text not null,
  organization_name text,
  city text,
  state text not null,
  address text,
  phone text,
  website text,
  accreditation_level text,
  status text,
  recognized_at date,
  expires_at date,
  pending_renewal boolean not null default false,
  source_url text not null,
  source_sha256 text not null,
  source_published_at date,
  imported_at timestamptz not null default now(),
  dedupe_key text not null
);

create unique index if not exists ra_entry_dedupe_idx
  on public.ra_entry (dataset, source_sha256, dedupe_key);

create index if not exists ra_entry_state_idx
  on public.ra_entry (state);

create index if not exists ra_entry_type_idx
  on public.ra_entry (entry_type);

create index if not exists ra_entry_name_trgm_idx
  on public.ra_entry using gin (name gin_trgm_ops);

create index if not exists ra_entry_org_name_trgm_idx
  on public.ra_entry using gin (organization_name gin_trgm_ops);

alter table public.ra_entry enable row level security;
alter table public.ra_import_run enable row level security;

-- Public read access for roster data + freshness.
do $$ begin
  create policy "ra_entry_select_public" on public.ra_entry
    for select
    to anon, authenticated
    using (true);
exception when duplicate_object then null; end $$;

do $$ begin
  create policy "ra_import_run_select_public" on public.ra_import_run
    for select
    to anon, authenticated
    using (true);
exception when duplicate_object then null; end $$;

-- Writes are expected to be performed by the ETL using a server role / direct DB access.

-- ---------------------------------------------------------------------------
-- Legal aid directory + partner portal
-- ---------------------------------------------------------------------------

create table if not exists public.aid_org (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  website text,
  phone text,
  email text,
  counties text[] not null default '{}',
  languages text[] not null default '{}',
  services text[] not null default '{}',
  intake_status text not null default 'waitlist' check (intake_status in ('open', 'waitlist', 'closed')),
  next_clinic_at timestamptz,
  eta_callback_days integer,
  updated_at timestamptz not null default now()
);

create table if not exists public.aid_org_membership (
  user_id uuid not null references auth.users (id) on delete cascade,
  org_id uuid not null references public.aid_org (id) on delete cascade,
  role text not null default 'editor' check (role in ('admin', 'editor')),
  created_at timestamptz not null default now(),
  primary key (user_id, org_id)
);

create index if not exists aid_org_membership_org_idx
  on public.aid_org_membership (org_id);

-- Keep updated_at fresh
create or replace function public.set_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

drop trigger if exists trg_aid_org_set_updated_at on public.aid_org;
create trigger trg_aid_org_set_updated_at
before update on public.aid_org
for each row execute function public.set_updated_at();

alter table public.aid_org enable row level security;
alter table public.aid_org_membership enable row level security;

-- Public directory reads
do $$ begin
  create policy "aid_org_select_public" on public.aid_org
    for select
    to anon, authenticated
    using (true);
exception when duplicate_object then null; end $$;

-- Membership reads for the logged-in user
do $$ begin
  create policy "aid_org_membership_select_self" on public.aid_org_membership
    for select
    to authenticated
    using (user_id = auth.uid());
exception when duplicate_object then null; end $$;

-- Only members can update their org rows
do $$ begin
  create policy "aid_org_update_for_members" on public.aid_org
    for update
    to authenticated
    using (
      exists (
        select 1
        from public.aid_org_membership m
        where m.org_id = id
          and m.user_id = auth.uid()
      )
    )
    with check (
      exists (
        select 1
        from public.aid_org_membership m
        where m.org_id = id
          and m.user_id = auth.uid()
      )
    );
exception when duplicate_object then null; end $$;

