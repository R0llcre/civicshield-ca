-- RegShield CA - partner portal hardening
-- - Restrict authenticated updates to allowed columns
-- - Add audit log for partner updates

-- Explicit grants (RLS still applies)
grant select on table public.ra_entry to anon, authenticated;
grant select on table public.ra_import_run to anon, authenticated;
grant select on table public.aid_org to anon, authenticated;
grant select on table public.aid_org_membership to authenticated;

-- Column-level update permissions for partners (RLS is enforced separately).
revoke update on table public.aid_org from authenticated;
grant update (intake_status, next_clinic_at, eta_callback_days, languages, counties)
  on table public.aid_org
  to authenticated;

-- ---------------------------------------------------------------------------
-- Audit table
-- ---------------------------------------------------------------------------

create table if not exists public.aid_org_update_audit (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.aid_org (id) on delete cascade,
  user_id uuid references auth.users (id) on delete set null,
  changed_at timestamptz not null default now(),
  patch jsonb not null,
  request_meta jsonb
);

alter table public.aid_org_update_audit enable row level security;

create or replace function public.audit_aid_org_update()
returns trigger as $$
declare
  diff jsonb := '{}'::jsonb;
  headers jsonb;
begin
  if new.intake_status is distinct from old.intake_status then
    diff := diff || jsonb_build_object(
      'intake_status', jsonb_build_object('from', old.intake_status, 'to', new.intake_status)
    );
  end if;
  if new.next_clinic_at is distinct from old.next_clinic_at then
    diff := diff || jsonb_build_object(
      'next_clinic_at', jsonb_build_object('from', old.next_clinic_at, 'to', new.next_clinic_at)
    );
  end if;
  if new.eta_callback_days is distinct from old.eta_callback_days then
    diff := diff || jsonb_build_object(
      'eta_callback_days', jsonb_build_object('from', old.eta_callback_days, 'to', new.eta_callback_days)
    );
  end if;
  if new.languages is distinct from old.languages then
    diff := diff || jsonb_build_object(
      'languages', jsonb_build_object('from', old.languages, 'to', new.languages)
    );
  end if;
  if new.counties is distinct from old.counties then
    diff := diff || jsonb_build_object(
      'counties', jsonb_build_object('from', old.counties, 'to', new.counties)
    );
  end if;

  if diff <> '{}'::jsonb then
    headers := nullif(current_setting('request.headers', true), '')::jsonb;
    insert into public.aid_org_update_audit (org_id, user_id, patch, request_meta)
    values (
      new.id,
      auth.uid(),
      diff,
      case
        when headers is null then null
        else jsonb_build_object(
          'user-agent', headers->>'user-agent',
          'x-forwarded-for', headers->>'x-forwarded-for'
        )
      end
    );
  end if;

  return new;
end;
$$ language plpgsql;

drop trigger if exists trg_aid_org_audit_update on public.aid_org;
create trigger trg_aid_org_audit_update
after update on public.aid_org
for each row execute function public.audit_aid_org_update();

