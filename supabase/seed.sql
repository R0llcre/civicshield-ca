-- RegShield CA - demo seed data
-- Replace these with verified organizations before any real pilot.

insert into public.aid_org
  (name, website, phone, email, counties, languages, services, intake_status, eta_callback_days, source_url, verified_at, verified_by)
values
  ('Sample Legal Aid 01 (Demo)', 'https://example.org', '(555) 010-0001', null, array['Los Angeles'], array['English','Spanish'], array['Asylum','Family'], 'waitlist', 5, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 02 (Demo)', 'https://example.org', '(555) 010-0002', null, array['San Diego'], array['English','Spanish','Chinese'], array['VAWA','Work authorization'], 'open', 3, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 03 (Demo)', 'https://example.org', '(555) 010-0003', null, array['Orange'], array['English','Korean'], array['Naturalization'], 'open', 2, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 04 (Demo)', 'https://example.org', '(555) 010-0004', null, array['San Francisco'], array['English','Chinese'], array['Removal defense'], 'waitlist', 7, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 05 (Demo)', 'https://example.org', '(555) 010-0005', null, array['Santa Clara'], array['English','Spanish','Vietnamese'], array['Family'], 'open', 2, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 06 (Demo)', 'https://example.org', '(555) 010-0006', null, array['Alameda'], array['English','Spanish'], array['Asylum'], 'closed', null, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 07 (Demo)', 'https://example.org', '(555) 010-0007', null, array['Sacramento'], array['English','Spanish'], array['Naturalization','Family'], 'waitlist', 10, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 08 (Demo)', 'https://example.org', '(555) 010-0008', null, array['San Mateo'], array['English','Chinese'], array['Work authorization'], 'open', 4, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 09 (Demo)', 'https://example.org', '(555) 010-0009', null, array['Riverside'], array['English','Spanish'], array['Removal defense'], 'waitlist', 8, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 10 (Demo)', 'https://example.org', '(555) 010-0010', null, array['San Bernardino'], array['English','Spanish'], array['Asylum','VAWA'], 'open', 3, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 11 (Demo)', 'https://example.org', '(555) 010-0011', null, array['Fresno'], array['English','Spanish'], array['Family'], 'waitlist', 12, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 12 (Demo)', 'https://example.org', '(555) 010-0012', null, array['Ventura'], array['English','Spanish'], array['Naturalization'], 'open', 2, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 13 (Demo)', 'https://example.org', '(555) 010-0013', null, array['Contra Costa'], array['English'], array['Asylum'], 'waitlist', 6, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 14 (Demo)', 'https://example.org', '(555) 010-0014', null, array['San Joaquin'], array['English','Spanish'], array['Work authorization'], 'open', 4, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 15 (Demo)', 'https://example.org', '(555) 010-0015', null, array['Sonoma'], array['English','Spanish'], array['Family'], 'open', 4, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 16 (Demo)', 'https://example.org', '(555) 010-0016', null, array['Monterey'], array['English','Spanish'], array['Asylum'], 'waitlist', 9, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 17 (Demo)', 'https://example.org', '(555) 010-0017', null, array['Santa Cruz'], array['English','Spanish'], array['Removal defense'], 'closed', null, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 18 (Demo)', 'https://example.org', '(555) 010-0018', null, array['Solano'], array['English'], array['Naturalization'], 'open', 2, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 19 (Demo)', 'https://example.org', '(555) 010-0019', null, array['Marin'], array['English','Spanish'], array['Family'], 'waitlist', 11, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 20 (Demo)', 'https://example.org', '(555) 010-0020', null, array['San Luis Obispo'], array['English','Spanish'], array['Asylum'], 'open', 3, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 21 (Demo)', 'https://example.org', '(555) 010-0021', null, array['Napa'], array['English','Spanish'], array['Work authorization'], 'waitlist', 6, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 22 (Demo)', 'https://example.org', '(555) 010-0022', null, array['Kern'], array['English','Spanish'], array['Removal defense'], 'waitlist', 7, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 23 (Demo)', 'https://example.org', '(555) 010-0023', null, array['Imperial'], array['English','Spanish'], array['Asylum','Family'], 'open', 3, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 24 (Demo)', 'https://example.org', '(555) 010-0024', null, array['Yolo'], array['English','Spanish'], array['Naturalization'], 'open', 2, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 25 (Demo)', 'https://example.org', '(555) 010-0025', null, array['Placer'], array['English'], array['Work authorization'], 'waitlist', 5, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 26 (Demo)', 'https://example.org', '(555) 010-0026', null, array['Stanislaus'], array['English','Spanish'], array['Family'], 'closed', null, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 27 (Demo)', 'https://example.org', '(555) 010-0027', null, array['Merced'], array['English','Spanish'], array['Asylum'], 'waitlist', 10, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 28 (Demo)', 'https://example.org', '(555) 010-0028', null, array['San Benito'], array['English','Spanish'], array['Removal defense'], 'open', 4, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 29 (Demo)', 'https://example.org', '(555) 010-0029', null, array['Santa Barbara'], array['English','Spanish'], array['Naturalization'], 'open', 2, 'https://example.org', now(), 'demo'),
  ('Sample Legal Aid 30 (Demo)', 'https://example.org', '(555) 010-0030', null, array['San Francisco'], array['English','Chinese'], array['Asylum','Removal defense'], 'waitlist', 9, 'https://example.org', now(), 'demo')
on conflict do nothing;
