-- RegShield CA - staging demo seed (Azure Postgres)
-- Idempotent: fixed UUIDs + upsert by primary key.

insert into public.aid_org (
  id,
  name,
  website,
  phone,
  email,
  counties,
  languages,
  services,
  intake_status,
  next_clinic_at,
  eta_callback_days,
  source_url,
  verified_at,
  verified_by,
  updated_at
)
values
(
  '11111111-1111-1111-1111-111111111111',
  'Sample Legal Aid (Staging Demo)',
  'https://example.org',
  '(555) 010-0101',
  null,
  array['Los Angeles'],
  array['English','Spanish'],
  array['Asylum','Family'],
  'waitlist',
  null,
  5,
  'https://example.org',
  now(),
  'demo',
  now()
),
(
  '22222222-2222-2222-2222-222222222222',
  'Community Immigration Clinic (Staging Demo)',
  'https://example.org',
  '(555) 010-0202',
  null,
  array['San Diego'],
  array['English','Spanish','Chinese'],
  array['Work authorization','VAWA'],
  'open',
  now() + interval '7 days',
  null,
  'https://example.org',
  now(),
  'demo',
  now()
)
on conflict (id)
do update set
  name = excluded.name,
  website = excluded.website,
  phone = excluded.phone,
  email = excluded.email,
  counties = excluded.counties,
  languages = excluded.languages,
  services = excluded.services,
  intake_status = excluded.intake_status,
  next_clinic_at = excluded.next_clinic_at,
  eta_callback_days = excluded.eta_callback_days,
  source_url = excluded.source_url,
  verified_at = excluded.verified_at,
  verified_by = excluded.verified_by,
  updated_at = excluded.updated_at;

