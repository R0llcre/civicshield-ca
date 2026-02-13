-- RegShield CA - aid_org provenance + query indexes (portable)

alter table public.aid_org
  add column if not exists source_url text,
  add column if not exists verified_at timestamptz,
  add column if not exists verified_by text,
  add column if not exists notes text;

-- Help directory queries filter on text[] fields; add GIN indexes for stable performance.
create index if not exists aid_org_counties_gin_idx
  on public.aid_org using gin (counties);

create index if not exists aid_org_languages_gin_idx
  on public.aid_org using gin (languages);

create index if not exists aid_org_services_gin_idx
  on public.aid_org using gin (services);

