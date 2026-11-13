-- Run only after 001_core.sql and 002_recurring_events.sql.
-- 003_live_match.sql — online provisional scoreboard for authorized event participants.
begin;
create table if not exists public.live_matches(
  event_id uuid primary key references public.events(id) on delete cascade,
  state text not null default 'READY' check(state in ('READY','LIVE','PAUSED','FINISHED')),
  team_a text not null default 'Time Amarelo',
  team_b text not null default 'Time Preto',
  elapsed_ms bigint not null default 0 check(elapsed_ms>=0),
  running_since timestamptz,
  updated_at timestamptz not null default now()
);
create table if not exists public.live_match_events(
  id bigint generated always as identity primary key,
  event_id uuid not null references public.events(id) on delete cascade,
  action text not null check(action in ('GOAL','YELLOW','RED','SUB')),
  side integer not null check(side in (0,1)),
  player_id uuid not null references auth.users(id),
  second_player_id uuid references auth.users(id),
  minute integer not null check(minute between 0 and 240),
  nonce uuid not null unique,
  created_at timestamptz not null default now()
);
create index if not exists live_events_by_match on public.live_match_events(event_id,id desc);
alter table public.live_matches enable row level security;
alter table public.live_match_events enable row level security;
revoke all on public.live_matches,public.live_match_events from anon,authenticated;

-- Account ownership, never local test-role selection, controls all modifications.
create or replace function public.live_event_owner(p_event uuid) returns boolean
language sql stable security definer set search_path=public,pg_temp
as $$
  select auth.uid() is not null and exists(
    select 1 from public.events e join public.groups g on g.id=e.group_id
    where e.id=p_event and g.owner_id=auth.uid()
  )
$$;

create or replace function public.live_match_state(p_event uuid,p_action text)
returns jsonb
language plpgsql security definer set search_path=public,pg_temp
as $$
declare match public.live_matches%rowtype; now_at timestamptz:=clock_timestamp(); delta bigint;
begin
  if not public.live_event_owner(p_event) then raise exception 'Somente o organizador controla o placar.';end if;
  if p_action not in ('LIVE','PAUSED','FINISHED') or p_action is null then raise exception 'Ação inválida.';end if;
  -- Serialize concurrent owner controls for the same event.
  perform 1 from public.events where id=p_event for update;
  insert into public.live_matches(event_id)values(p_event)on conflict do nothing;
  select * into match from public.live_matches where event_id=p_event for update;
  if match.state='FINISHED' then raise exception 'A partida já foi encerrada.';end if;
  if match.state='READY' and p_action='PAUSED' then raise exception 'Inicie a partida primeiro.';end if;
  if match.state='LIVE' and p_action<>'LIVE' then
    delta:=greatest(0,(extract(epoch from(now_at-match.running_since))*1000)::bigint);
    match.elapsed_ms:=match.elapsed_ms+delta;
    match.running_since:=null;
  elsif match.state<>'LIVE' and p_action='LIVE' then
    match.running_since:=now_at;
  end if;
  match.state:=p_action;
  update public.live_matches set state=match.state,elapsed_ms=match.elapsed_ms,
    running_since=match.running_since,updated_at=now_at where event_id=p_event;
  return jsonb_build_object('state',match.state,'elapsed_ms',match.elapsed_ms);
end $$;

create or replace function public.record_live_event(
  p_event uuid,p_action text,p_side integer,p_player uuid,
  p_second_player uuid,p_minute integer,p_nonce uuid
) returns jsonb
language plpgsql security definer set search_path=public,pg_temp
as $$
declare match public.live_matches%rowtype;record_id bigint;
begin
  if not public.live_event_owner(p_event) then raise exception 'Somente o organizador registra lances.';end if;
  if p_action not in ('GOAL','YELLOW','RED','SUB') or p_action is null
    or p_side not in (0,1) or p_side is null
    or p_minute not between 0 and 240 or p_minute is null
    or p_nonce is null then raise exception 'Lance inválido.';end if;
  select * into match from public.live_matches where event_id=p_event for update;
  if match.state<>'LIVE' and match.state<>'PAUSED'
    then raise exception 'Inicie a partida antes de registrar lances.';end if;
  if not exists(select 1 from public.event_attendance
     where event_id=p_event and user_id=p_player and status='CONFIRMED')
    then raise exception 'Jogador não confirmado no evento.';end if;
  if p_action='SUB' then
    if p_second_player is null or p_second_player=p_player or
       not exists(select 1 from public.event_attendance
          where event_id=p_event and user_id=p_second_player and status='CONFIRMED')
       then raise exception 'Informe o substituto confirmado.';end if;
  elsif p_second_player is not null then
    raise exception 'Apenas substituições admitem um segundo participante nesta versão.';
  end if;
  insert into public.live_match_events(event_id,action,side,player_id,second_player_id,minute,nonce)
    values(p_event,p_action,p_side,p_player,p_second_player,p_minute,p_nonce)
  on conflict(nonce) do nothing returning id into record_id;
  return jsonb_build_object('registered',record_id is not null);
end $$;

create or replace function public.live_match_snapshot(p_event uuid)
returns jsonb
language plpgsql stable security definer set search_path=public,pg_temp
as $$
declare event_data public.events%rowtype;match public.live_matches%rowtype;
 owner boolean;players jsonb;timeline jsonb;score_a integer;score_b integer;elapsed bigint;
begin
  if auth.uid() is null then raise exception 'Entre na sua conta.';end if;
  select * into event_data from public.events where id=p_event;
  if not found then raise exception 'Evento inexistente.';end if;
  owner:=public.live_event_owner(p_event);
  if not owner and not exists(select 1 from public.event_attendance
      where event_id=p_event and user_id=auth.uid()) then
      raise exception 'Você não participa deste evento.';end if;
  select * into match from public.live_matches where event_id=p_event;
  select coalesce(jsonb_agg(jsonb_build_object('id',a.user_id,'name',p.display_name,
    'position',a.position) order by p.display_name),'[]'::jsonb)
    into players from public.event_attendance a join public.profiles p on p.id=a.user_id
    where a.event_id=p_event and a.status='CONFIRMED';
  select coalesce(count(*) filter(where action='GOAL' and side=0),0),
         coalesce(count(*) filter(where action='GOAL' and side=1),0)
    into score_a,score_b from public.live_match_events where event_id=p_event;
  select coalesce(jsonb_agg(jsonb_build_object(
    'id',q.id,'action',q.action,'side',q.side,'minute',q.minute,
    'player',p.display_name,'other',coalesce(second.display_name,'')) order by q.id desc),
    '[]'::jsonb) into timeline
    from (select * from public.live_match_events where event_id=p_event order by id desc limit 60) q
    join public.profiles p on p.id=q.player_id
    left join public.profiles second on second.id=q.second_player_id;
  elapsed:=coalesce(match.elapsed_ms,0);
  if match.state='LIVE' and match.running_since is not null then
    elapsed:=elapsed+greatest(0,(extract(epoch from(clock_timestamp()-match.running_since))*1000)::bigint);
  end if;
  return jsonb_build_object(
    'event_id',p_event,'title',event_data.title,'owner',owner,
    'state',coalesce(match.state,'READY'),'team_a',coalesce(match.team_a,'Time Amarelo'),
    'team_b',coalesce(match.team_b,'Time Preto'),'elapsed_ms',elapsed,
    'score_a',score_a,'score_b',score_b,'players',players,'timeline',timeline);
end $$;

revoke all on function public.live_event_owner(uuid) from PUBLIC;
revoke all on function public.live_match_state(uuid,text) from PUBLIC;
revoke all on function public.record_live_event(uuid,text,integer,uuid,uuid,integer,uuid) from PUBLIC;
revoke all on function public.live_match_snapshot(uuid) from PUBLIC;
grant execute on function public.live_event_owner(uuid) to authenticated;
grant execute on function public.live_match_state(uuid,text) to authenticated;
grant execute on function public.record_live_event(uuid,text,integer,uuid,uuid,integer,uuid) to authenticated;
grant execute on function public.live_match_snapshot(uuid) to authenticated;
commit;
