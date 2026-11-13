-- Pelada League: PostgreSQL / Supabase foundation for real multiuser events.
-- Execute once in a FREE Supabase project SQL Editor after reviewing the policies.
-- Requires auth.users, postgres/pgcrypto. Never embed service_role in Android.
begin;
create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text not null check (char_length(trim(display_name)) between 2 and 80),
  primary_position text not null default 'LIN' check (primary_position in ('GOL','ZAG','MEI','ATA','LIN')),
  created_at timestamptz not null default now()
);

create or replace function public.create_player_profile() returns trigger
language plpgsql security definer set search_path = public, pg_temp
as $$
declare v_position text; v_name text;
begin
  v_name := coalesce(nullif(left(trim(new.raw_user_meta_data->>'display_name'),80),''),
            split_part(coalesce(new.email,'jogador'), '@',1));
  if char_length(v_name)<2 then v_name := 'Jogador'; end if;
  v_position := new.raw_user_meta_data->>'primary_position';
  if v_position not in ('GOL','ZAG','MEI','ATA','LIN') or v_position is null then
    v_position := 'LIN';
  end if;
  insert into public.profiles(id,display_name,primary_position)
    values(new.id,v_name,v_position) on conflict(id) do nothing;
  return new;
end $$;

drop trigger if exists on_player_signup on auth.users;
create trigger on_player_signup after insert on auth.users
  for each row execute function public.create_player_profile();

create table if not exists public.groups (
  id uuid primary key default gen_random_uuid(),
  name text not null check (char_length(trim(name)) between 3 and 90),
  owner_id uuid not null references auth.users(id) on delete restrict,
  created_at timestamptz not null default now()
);

create table if not exists public.group_members (
  group_id uuid not null references public.groups(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  role text not null check(role in ('ORGANIZER','TREASURER','PLAYER','CAPTAIN','COACH')),
  primary key(group_id,user_id)
);

create table if not exists public.events (
  id uuid primary key default gen_random_uuid(),
  group_id uuid not null references public.groups(id) on delete cascade,
  title text not null check(char_length(trim(title)) between 3 and 100),
  location text not null check(char_length(trim(location)) between 2 and 160),
  starts_at timestamptz not null,
  capacity integer not null check(capacity between 2 and 100),
  fee_cents integer not null default 0 check(fee_cents between 0 and 10000000),
  field_cost_cents integer not null default 0 check(field_cost_cents between 0 and 10000000),
  goalkeeper_exempt boolean not null default true,
  pix_key text not null default '' check(char_length(pix_key)<=160),
  invite_code text unique not null default encode(gen_random_bytes(16),'hex'),
  invite_enabled boolean not null default true,
  status text not null default 'OPEN' check(status in ('OPEN','CLOSED','FINISHED')),
  created_at timestamptz not null default now()
);

create table if not exists public.event_attendance (
  event_id uuid not null references public.events(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  position text not null check(position in ('GOL','ZAG','MEI','ATA','LIN')),
  status text not null check(status in ('CONFIRMED','WAITLIST','DECLINED')),
  requested_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key(event_id,user_id)
);
create index if not exists event_attendance_queue
  on public.event_attendance(event_id,status,requested_at,user_id);

create table if not exists public.event_exemptions (
  event_id uuid not null references public.events(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  override boolean not null,
  primary key(event_id,user_id)
);

create table if not exists public.event_payments (
  event_id uuid not null references public.events(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  paid_cents integer not null default 0 check(paid_cents between 0 and 10000000),
  method text not null default 'MANUAL' check(method in ('MANUAL','PIX','DINHEIRO','OUTRO','AJUSTE')),
  recorded_by uuid not null references auth.users(id),
  updated_at timestamptz not null default now(),
  primary key(event_id,user_id)
);
create table if not exists public.event_payment_log (
  id bigint generated always as identity primary key,
  event_id uuid not null references public.events(id) on delete cascade,
  user_id uuid not null references auth.users(id),
  actor_id uuid not null references auth.users(id),
  delta_cents integer not null,
  method text not null,
  occurred_at timestamptz not null default now()
);

-- SECURITY DEFINER read helpers prevent circular RLS policy evaluation.
create or replace function public.is_group_owner(p_group uuid) returns boolean
language sql stable security definer set search_path=public, pg_temp
as $ select auth.uid() is not null and exists(
    select 1 from public.groups where id=p_group and owner_id=auth.uid()
  ) $;
create or replace function public.is_event_owner(p_event uuid) returns boolean
language sql stable security definer set search_path=public, pg_temp
as $ select auth.uid() is not null and exists(
    select 1 from public.events e join public.groups g on g.id=e.group_id
    where e.id=p_event and g.owner_id=auth.uid()
  ) $;
create or replace function public.is_event_attendee(p_event uuid) returns boolean
language sql stable security definer set search_path=public, pg_temp
as $ select auth.uid() is not null and exists(
    select 1 from public.event_attendance a
    where a.event_id=p_event and a.user_id=auth.uid()
  ) $;

alter table public.profiles enable row level security;
alter table public.groups enable row level security;
alter table public.group_members enable row level security;
alter table public.events enable row level security;
alter table public.event_attendance enable row level security;
alter table public.event_exemptions enable row level security;
alter table public.event_payments enable row level security;
alter table public.event_payment_log enable row level security;

drop policy if exists profiles_read on public.profiles;
create policy profiles_read on public.profiles for select to authenticated
  using(id=auth.uid());
drop policy if exists profiles_edit on public.profiles;
create policy profiles_edit on public.profiles for update to authenticated
  using(id=auth.uid()) with check(id=auth.uid());

drop policy if exists groups_owner_read on public.groups;
create policy groups_owner_read on public.groups for select to authenticated
  using(owner_id=auth.uid());

drop policy if exists membership_self_or_owner on public.group_members;
create policy membership_self_or_owner on public.group_members for select to authenticated
  using(user_id=auth.uid() or public.is_group_owner(group_id));

drop policy if exists events_visible_to_participant on public.events;
create policy events_visible_to_participant on public.events for select to authenticated
  using(public.is_group_owner(group_id) or public.is_event_attendee(id));

drop policy if exists attendance_self_or_owner on public.event_attendance;
create policy attendance_self_or_owner on public.event_attendance for select to authenticated
  using(user_id=auth.uid() or public.is_event_owner(event_id));

drop policy if exists payments_self_or_owner on public.event_payments;
create policy payments_self_or_owner on public.event_payments for select to authenticated
  using(user_id=auth.uid() or public.is_event_owner(event_id));
drop policy if exists exceptions_self_or_owner on public.event_exemptions;
create policy exceptions_self_or_owner on public.event_exemptions for select to authenticated
  using(user_id=auth.uid() or public.is_event_owner(event_id));
drop policy if exists payment_log_owner on public.event_payment_log;
create policy payment_log_owner on public.event_payment_log for select to authenticated
  using(public.is_event_owner(event_id));

revoke all on public.profiles,public.groups,public.group_members,public.events,
  public.event_attendance,public.event_exemptions,public.event_payments,public.event_payment_log
  from anon;
grant select,update on public.profiles to authenticated;
grant select on public.groups,public.group_members,public.events,public.event_attendance,
  public.event_exemptions,public.event_payments,public.event_payment_log to authenticated;

-- All group/event creation and attendance/payment writes go through checked RPCs.
create or replace function public.create_group(p_name text) returns jsonb
language plpgsql security definer set search_path=public, pg_temp
as $$
declare actor uuid:=auth.uid();gid uuid;
begin
  if actor is null then raise exception 'Entre na sua conta.'; end if;
  if p_name is null or char_length(trim(p_name)) not between 3 and 90
    then raise exception 'Informe um nome válido para a pelada.'; end if;
  insert into public.groups(name,owner_id)values(trim(p_name),actor)returning id into gid;
  insert into public.group_members(group_id,user_id,role)values(gid,actor,'ORGANIZER');
  return jsonb_build_object('id',gid);
end $;

create or replace function public.create_event(
 p_group uuid,p_title text,p_location text,p_starts_at timestamptz,
 p_capacity integer,p_fee_cents integer,p_field_cost_cents integer,
 p_keeper_exempt boolean,p_pix_key text default ''
) returns jsonb
language plpgsql security definer set search_path=public, pg_temp
as $$
declare actor uuid:=auth.uid();ev public.events%rowtype;
begin
  if actor is null or not exists(select 1 from public.groups
     where id=p_group and owner_id=actor) then
     raise exception 'Somente o organizador pode criar o evento.';end if;
  if p_title is null or char_length(trim(p_title)) not between 3 and 100
     or p_location is null or char_length(trim(p_location)) not between 2 and 160
     or p_starts_at is null or p_capacity not between 2 and 100
     or p_fee_cents not between 0 and 10000000
     or p_field_cost_cents not between 0 and 10000000
     or char_length(coalesce(p_pix_key,''))>160 then
     raise exception 'Dados do evento inválidos.';end if;
  insert into public.events(group_id,title,location,starts_at,capacity,
     fee_cents,field_cost_cents,goalkeeper_exempt,pix_key)
  values(p_group,trim(p_title),trim(p_location),p_starts_at,p_capacity,
    p_fee_cents,p_field_cost_cents,coalesce(p_keeper_exempt,true),coalesce(p_pix_key,''))
  returning * into ev;
  return jsonb_build_object('id',ev.id,'code',ev.invite_code,'title',ev.title,
    'starts_at',ev.starts_at,'capacity',ev.capacity);
end $$;

-- Invite code is a 128-bit bearer token for discovering the event only.
-- It does not authorize editing participants, payments or reports.
create or replace function public.event_preview(p_code text) returns jsonb
language plpgsql security definer set search_path=public, pg_temp
as $$
declare ev public.events%rowtype; grp text; confirmed integer; waiting integer; mine text; actor uuid:=auth.uid();
begin
  if actor is null then raise exception 'Entre na sua conta para abrir o convite.';end if;
  select * into ev from public.events where invite_code=p_code and invite_enabled=true;
  if not found then raise exception 'Convite inexistente ou desativado.';end if;
  select name into grp from public.groups where id=ev.group_id;
  select count(*) into confirmed from public.event_attendance
     where event_id=ev.id and status='CONFIRMED';
  select count(*) into waiting from public.event_attendance
     where event_id=ev.id and status='WAITLIST';
  select status into mine from public.event_attendance
     where event_id=ev.id and user_id=actor;
  return jsonb_build_object('id',ev.id,'group',grp,'title',ev.title,
    'location',ev.location,'starts_at',ev.starts_at,'capacity',ev.capacity,
    'confirmed',confirmed,'waiting',waiting,'status',ev.status,
    'my_status',coalesce(mine,'PENDING'),'fee_cents',ev.fee_cents,
    'goalkeeper_exempt',ev.goalkeeper_exempt);
end $$;

create or replace function public.event_respond(p_code text,p_going boolean,p_position text default null)
returns jsonb
language plpgsql security definer set search_path=public, pg_temp
as $$
declare actor uuid:=auth.uid();ev public.events%rowtype; prior text; next_status text;
  player_position text; count_confirmed integer; promoted uuid;
begin
  if actor is null then raise exception 'Entre na sua conta.';end if;
  select * into ev from public.events
    where invite_code=p_code and invite_enabled=true for update;
  if not found then raise exception 'Convite inválido.';end if;
  if ev.status<>'OPEN' then raise exception 'Inscrições encerradas.';end if;
  select primary_position into player_position from public.profiles where id=actor;
  player_position:=coalesce(p_position,player_position);
  if player_position not in ('GOL','ZAG','MEI','ATA','LIN') or player_position is null
    then raise exception 'Selecione sua função na partida.';end if;
  select status into prior from public.event_attendance
    where event_id=ev.id and user_id=actor;
  if not p_going then next_status:='DECLINED';
  elsif prior='CONFIRMED' then next_status:='CONFIRMED';
  else
    select count(*) into count_confirmed from public.event_attendance
      where event_id=ev.id and status='CONFIRMED';
    next_status:=case when count_confirmed<ev.capacity then 'CONFIRMED' else 'WAITLIST' end;
  end if;
  insert into public.event_attendance(event_id,user_id,status,position,requested_at)
  values(ev.id,actor,next_status,player_position,clock_timestamp())
  on conflict(event_id,user_id)do update
   set status=excluded.status,position=excluded.position,updated_at=clock_timestamp(),
       requested_at=case when public.event_attendance.status in ('DECLINED')
         then clock_timestamp() else public.event_attendance.requested_at end;
  if prior='CONFIRMED' and next_status<>'CONFIRMED' then
    select user_id into promoted from public.event_attendance
      where event_id=ev.id and status='WAITLIST'
      order by requested_at,user_id limit 1;
    if promoted is not null then
      update public.event_attendance set status='CONFIRMED',updated_at=clock_timestamp()
        where event_id=ev.id and user_id=promoted;
    end if;
  end if;
  return jsonb_build_object('status',next_status,'event_id',ev.id);
end $$;

create or replace function public.mark_event_payment(
 p_event uuid,p_user uuid,p_total_cents integer,p_method text
) returns jsonb
language plpgsql security definer set search_path=public, pg_temp
as $$
declare actor uuid:=auth.uid();ev public.events%rowtype;g_role text;
  participant_position text; override boolean; due integer;before_paid integer;
begin
  if actor is null then raise exception 'Entre na sua conta.';end if;
  select * into ev from public.events where id=p_event for update;
  if not found then raise exception 'Evento não encontrado.';end if;
  select role into g_role from public.group_members
    where group_id=ev.group_id and user_id=actor;
  if g_role not in ('ORGANIZER','TREASURER') or g_role is null
    then raise exception 'Somente o responsável financeiro pode registrar pagamentos.';end if;
  select position into participant_position from public.event_attendance
    where event_id=p_event and user_id=p_user and status='CONFIRMED';
  if not found then raise exception 'Jogador não está confirmado.';end if;
  select x.override into override from public.event_exemptions x
    where x.event_id=p_event and x.user_id=p_user;
  due:=case when coalesce(override,ev.goalkeeper_exempt and participant_position='GOL')
       then 0 else ev.fee_cents end;
  if p_total_cents is null or p_total_cents<0 or p_total_cents>due
     or p_method not in ('PIX','DINHEIRO','OUTRO','AJUSTE')
    then raise exception 'Valor ou forma de pagamento inválida.';end if;
  select paid_cents into before_paid from public.event_payments
    where event_id=p_event and user_id=p_user;
  before_paid:=coalesce(before_paid,0);
  insert into public.event_payments(event_id,user_id,paid_cents,method,recorded_by)
    values(p_event,p_user,p_total_cents,p_method,actor)
  on conflict(event_id,user_id)do update set paid_cents=excluded.paid_cents,
    method=excluded.method,recorded_by=excluded.recorded_by,updated_at=now();
  if p_total_cents<>before_paid then
    insert into public.event_payment_log(event_id,user_id,actor_id,delta_cents,method)
      values(p_event,p_user,actor,p_total_cents-before_paid,p_method);
  end if;
  return jsonb_build_object('paid_cents',p_total_cents,'due_cents',due);
end $$;

create or replace function public.set_event_exemption(p_event uuid,p_user uuid,p_override boolean)
returns boolean
language plpgsql security definer set search_path=public, pg_temp
as $$
declare actor uuid:=auth.uid();g uuid;
begin
  select group_id into g from public.events where id=p_event;
  if actor is null or not exists(select 1 from public.group_members
    where group_id=g and user_id=actor and role in ('ORGANIZER','TREASURER'))
    then raise exception 'Somente o responsável financeiro pode alterar isenções.';end if;
  if not exists(select 1 from public.event_attendance where event_id=p_event and user_id=p_user)
    then raise exception 'Jogador não registrado.';end if;
  if p_override is null then
    delete from public.event_exemptions where event_id=p_event and user_id=p_user;
  else
    insert into public.event_exemptions(event_id,user_id,override)values(p_event,p_user,p_override)
    on conflict(event_id,user_id)do update set override=excluded.override;
  end if;
  return true;
end $$;

-- Only the organizer/treasurer can read a complete financial participant list.
create or replace function public.event_finance(p_event uuid) returns jsonb
language plpgsql security definer set search_path=public, pg_temp
as $
declare actor uuid:=auth.uid(); ev public.events%rowtype; members jsonb;
begin
  select * into ev from public.events where id=p_event;
  if not found then raise exception 'Evento não encontrado.';end if;
  if actor is null or not exists(select 1 from public.group_members
    where group_id=ev.group_id and user_id=actor and role in ('ORGANIZER','TREASURER'))
    then raise exception 'Sem permissão para consultar os pagamentos.';end if;
  select coalesce(jsonb_agg(jsonb_build_object(
    'user_id',a.user_id,'name',p.display_name,'position',a.position,
    'due_cents',case when coalesce(x.override,ev.goalkeeper_exempt and a.position='GOL')
      then 0 else ev.fee_cents end,
    'paid_cents',coalesce(pay.paid_cents,0)
  ) order by p.display_name),'[]'::jsonb) into members
  from public.event_attendance a
  join public.profiles p on p.id=a.user_id
  left join public.event_exemptions x on x.event_id=a.event_id and x.user_id=a.user_id
  left join public.event_payments pay on pay.event_id=a.event_id and pay.user_id=a.user_id
  where a.event_id=p_event and a.status='CONFIRMED';
  return jsonb_build_object('event_id',ev.id,'fee_cents',ev.fee_cents,
      'field_cost_cents',ev.field_cost_cents,'players',members);
end $;

revoke all on function public.is_group_owner(uuid) from PUBLIC;
revoke all on function public.is_event_owner(uuid) from PUBLIC;
revoke all on function public.is_event_attendee(uuid) from PUBLIC;
grant execute on function public.is_group_owner(uuid) to authenticated;
grant execute on function public.is_event_owner(uuid) to authenticated;
grant execute on function public.is_event_attendee(uuid) to authenticated;
revoke all on function public.create_player_profile() from PUBLIC;
revoke all on function public.create_group(text) from PUBLIC;
revoke all on function public.create_event(uuid,text,text,timestamptz,integer,integer,integer,boolean,text) from PUBLIC;
revoke all on function public.event_preview(text) from PUBLIC;
revoke all on function public.event_respond(text,boolean,text) from PUBLIC;
revoke all on function public.mark_event_payment(uuid,uuid,integer,text) from PUBLIC;
revoke all on function public.set_event_exemption(uuid,uuid,boolean) from PUBLIC;
revoke all on function public.event_finance(uuid) from PUBLIC;

grant execute on function public.create_group(text) to authenticated;
grant execute on function public.create_event(uuid,text,text,timestamptz,integer,integer,integer,boolean,text) to authenticated;
grant execute on function public.event_preview(text) to authenticated;
grant execute on function public.event_respond(text,boolean,text) to authenticated;
grant execute on function public.mark_event_payment(uuid,uuid,integer,text) to authenticated;
grant execute on function public.set_event_exemption(uuid,uuid,boolean) to authenticated;
grant execute on function public.event_finance(uuid) to authenticated;
commit;
