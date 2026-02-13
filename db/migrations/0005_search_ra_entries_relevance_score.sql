-- RegShield CA - roster search RPC relevance score (0..100) (portable)
--
-- Replaces the older raw score (similarity + boosts) with a user-facing
-- relevance percentage. This makes `score` directly usable in UI.

create or replace function public.search_ra_entries(
  q text,
  state text default 'CA',
  lim int default 20
)
returns table (
  entry_type text,
  name text,
  organization_name text,
  city text,
  state text,
  address text,
  phone text,
  website text,
  accreditation_level text,
  status text,
  expires_at date,
  pending_renewal boolean,
  source_url text,
  source_sha256 text,
  score real,
  matched_on text
)
language sql
stable
as $$
with latest as (
  select
    (select r.source_sha256
     from public.ra_import_run r
     where r.dataset = 'orgs_by_state_city' and r.status = 'success'
     order by r.ended_at desc nulls last
     limit 1) as org_sha,
    (select r.source_sha256
     from public.ra_import_run r
     where r.dataset = 'reps_roster' and r.status = 'success'
     order by r.ended_at desc nulls last
     limit 1) as rep_sha
),
input as (
  select
    trim(q) as q,
    lower(trim(q)) as ql,
    char_length(trim(q)) as qlen,
    upper(trim(state)) as st,
    greatest(1, least(lim, 50)) as lim
),
candidates as (
  select
    e.entry_type,
    e.name,
    e.organization_name,
    e.city,
    e.state,
    e.address,
    e.phone,
    e.website,
    e.accreditation_level,
    e.status,
    e.expires_at,
    e.pending_renewal,
    e.source_url,
    e.source_sha256,
    similarity(e.name, i.q) as sim_name,
    similarity(coalesce(e.organization_name, ''), i.q) as sim_org,
    greatest(
      similarity(e.name, i.q),
      similarity(coalesce(e.organization_name, ''), i.q)
    ) * 100.0 as sim_pct,
    case
      when lower(e.name) = i.ql then 100.0
      when lower(e.name) like i.ql || '%' then case when i.qlen < 4 then 70.0 else 85.0 end
      when lower(e.name) like '%' || i.ql || '%' then case when i.qlen < 4 then 55.0 else 65.0 end
      else 0.0
    end as floor_name,
    case
      when lower(coalesce(e.organization_name, '')) = i.ql then 95.0
      when lower(coalesce(e.organization_name, '')) like i.ql || '%' then case when i.qlen < 4 then 65.0 else 80.0 end
      when lower(coalesce(e.organization_name, '')) like '%' || i.ql || '%' then case when i.qlen < 4 then 50.0 else 60.0 end
      else 0.0
    end as floor_org,
    case
      when similarity(e.name, i.q) >= similarity(coalesce(e.organization_name, ''), i.q) then 'name'
      else 'organization_name'
    end as matched_on
  from public.ra_entry e
  cross join latest l
  cross join input i
  where char_length(i.q) >= 2
    and (
      (e.dataset = 'orgs_by_state_city'
        and e.source_sha256 = l.org_sha
        and e.entry_type = 'organization'
        and e.state = i.st)
      or
      (e.dataset = 'reps_roster'
        and e.source_sha256 = l.rep_sha
        and e.entry_type = 'representative')
    )
    and (
      e.name % i.q
      or coalesce(e.organization_name, '') % i.q
      or lower(e.name) like '%' || lower(i.q) || '%'
      or lower(coalesce(e.organization_name, '')) like '%' || lower(i.q) || '%'
    )
)
select
  entry_type,
  name,
  organization_name,
  city,
  state,
  address,
  phone,
  website,
  accreditation_level,
  status,
  expires_at,
  pending_renewal,
  source_url,
  source_sha256,
  least(
    100.0,
    greatest(
      0.0,
      greatest(sim_pct, greatest(floor_name, floor_org))
    )
  )::real as score,
  matched_on
from candidates
order by score desc, sim_pct desc, name asc
limit (select lim from input);
$$;

