-- RegShield CA - Partner security (portable, Entra External ID via OIDC)
-- Strategy:
-- - Application handles authentication and passes OIDC subject ("sub") + email.
-- - DB provides membership-aware update function to enforce allowed fields + audit.

create table if not exists public.partner_user (
  oidc_sub text primary key,
  email text not null,
  created_at timestamptz not null default now(),
  last_seen_at timestamptz not null default now()
);

create index if not exists partner_user_email_idx
  on public.partner_user (lower(email));

-- Invitations are DB-native so we do not require Graph API user provisioning.
create table if not exists public.partner_invite (
  id uuid primary key default gen_random_uuid(),
  email text not null,
  org_id uuid not null references public.aid_org (id) on delete cascade,
  role text not null default 'editor' check (role in ('admin', 'editor')),
  created_at timestamptz not null default now(),
  claimed_at timestamptz,
  claimed_sub text
);

create unique index if not exists partner_invite_email_org_unclaimed_uq
  on public.partner_invite (lower(email), org_id)
  where claimed_at is null;

-- Backfill constraint: enforce for new rows, but don't fail if legacy rows exist.
do $$
begin
  alter table public.aid_org_membership
    add constraint aid_org_membership_user_sub_fkey
    foreign key (user_sub) references public.partner_user (oidc_sub)
    on delete cascade
    not valid;
exception when duplicate_object then
  null;
end $$;

-- Claim invites on first login.
create or replace function public.fn_partner_upsert_user_and_claim_invites(
  p_email text,
  p_sub text
)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if p_email is null or length(trim(p_email)) = 0 then
    raise exception 'email required' using errcode = '22023';
  end if;
  if p_sub is null or length(trim(p_sub)) = 0 then
    raise exception 'sub required' using errcode = '22023';
  end if;

  insert into public.partner_user (oidc_sub, email, last_seen_at)
  values (p_sub, p_email, now())
  on conflict (oidc_sub)
  do update set
    email = excluded.email,
    last_seen_at = excluded.last_seen_at;

  -- Claim all outstanding invites for this email.
  with to_claim as (
    select id, org_id, role
    from public.partner_invite
    where lower(email) = lower(p_email)
      and claimed_at is null
    for update
  ),
  upsert_membership as (
    insert into public.aid_org_membership (user_sub, org_id, role)
    select p_sub, org_id, role
    from to_claim
    on conflict (user_sub, org_id)
    do update set role = excluded.role
    returning 1
  )
  update public.partner_invite i
  set claimed_at = now(), claimed_sub = p_sub
  where i.id in (select id from to_claim);
end;
$$;

-- Partner-safe update function: whitelist fields + membership check + audit.
create or replace function public.fn_partner_update_aid_org(
  p_org_id uuid,
  p_user_sub text,
  p_patch jsonb,
  p_request_meta jsonb default null
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  k text;
  allowed_keys text[] := array['intake_status','next_clinic_at','eta_callback_days','languages','counties'];
  _languages text[];
  _counties text[];
begin
  if p_org_id is null then
    raise exception 'org_id required' using errcode = '22023';
  end if;
  if p_user_sub is null or length(trim(p_user_sub)) = 0 then
    raise exception 'sub required' using errcode = '22023';
  end if;
  if p_patch is null or jsonb_typeof(p_patch) <> 'object' then
    raise exception 'patch must be an object' using errcode = '22023';
  end if;

  if not exists (
    select 1
    from public.aid_org_membership m
    where m.org_id = p_org_id
      and m.user_sub = p_user_sub
  ) then
    raise exception 'forbidden' using errcode = '42501';
  end if;

  -- Reject unknown keys to prevent accidental widening of the surface area.
  for k in select jsonb_object_keys(p_patch)
  loop
    if not (k = any(allowed_keys)) then
      raise exception 'invalid patch key: %', k using errcode = '22023';
    end if;
  end loop;

  if p_patch ? 'languages' then
    if jsonb_typeof(p_patch->'languages') <> 'array' then
      raise exception 'languages must be array' using errcode = '22023';
    end if;
    select coalesce(array_agg(v), '{}') into _languages
    from (
      select nullif(trim(value), '') as v
      from jsonb_array_elements_text(p_patch->'languages') t(value)
    ) s
    where v is not null;
  end if;

  if p_patch ? 'counties' then
    if jsonb_typeof(p_patch->'counties') <> 'array' then
      raise exception 'counties must be array' using errcode = '22023';
    end if;
    select coalesce(array_agg(v), '{}') into _counties
    from (
      select nullif(trim(value), '') as v
      from jsonb_array_elements_text(p_patch->'counties') t(value)
    ) s
    where v is not null;
  end if;

  update public.aid_org o
  set
    intake_status = case
      when p_patch ? 'intake_status' then coalesce(nullif(trim(p_patch->>'intake_status'), ''), o.intake_status)
      else o.intake_status
    end,
    next_clinic_at = case
      when p_patch ? 'next_clinic_at' then nullif(p_patch->>'next_clinic_at', '')::timestamptz
      else o.next_clinic_at
    end,
    eta_callback_days = case
      when p_patch ? 'eta_callback_days' then nullif(p_patch->>'eta_callback_days', '')::int
      else o.eta_callback_days
    end,
    languages = case
      when p_patch ? 'languages' then _languages
      else o.languages
    end,
    counties = case
      when p_patch ? 'counties' then _counties
      else o.counties
    end
  where o.id = p_org_id;

  if not found then
    raise exception 'org not found' using errcode = '22023';
  end if;

  insert into public.aid_org_update_audit (org_id, user_sub, patch, request_meta)
  values (p_org_id, p_user_sub, p_patch, p_request_meta);
end;
$$;

