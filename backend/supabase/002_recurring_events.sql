-- 002_recurring_events.sql — run after 001_core.sql, on the user's FREE Supabase project.
begin;
create or replace function public.create_weekly_events(
 p_group uuid,p_title text,p_location text,p_starts_at timestamptz,
 p_capacity integer,p_fee_cents integer,p_field_cost_cents integer,
 p_keeper_exempt boolean,p_pix_key text,p_weeks integer
) returns jsonb
language plpgsql security definer set search_path=public,pg_temp
as $$
declare actor uuid:=auth.uid();n integer;first_code text;
begin
  if actor is null or not exists(
      select 1 from public.groups where id=p_group and owner_id=actor
  ) then raise exception 'Somente o organizador pode criar eventos semanais.';end if;
  if p_weeks is null or p_weeks not between 2 and 12
     then raise exception 'Escolha de 2 a 12 semanas.';end if;
  if p_starts_at is null or p_starts_at<=now()
     then raise exception 'Escolha uma data futura.';end if;
  if p_title is null or char_length(trim(p_title)) not between 3 and 100
     or p_location is null or char_length(trim(p_location)) not between 2 and 160
     or p_capacity is null or p_capacity not between 2 and 100
     or p_fee_cents is null or p_fee_cents not between 0 and 10000000
     or p_field_cost_cents is null or p_field_cost_cents not between 0 and 10000000
     or char_length(coalesce(p_pix_key,''))>160
     then raise exception 'Dados do evento inválidos.';end if;
  for n in 0..p_weeks-1 loop
    if n=0 then
      insert into public.events(
        group_id,title,location,starts_at,capacity,fee_cents,field_cost_cents,
        goalkeeper_exempt,pix_key
      )values(
        p_group,trim(p_title),trim(p_location),p_starts_at,
        p_capacity,p_fee_cents,p_field_cost_cents,coalesce(p_keeper_exempt,true),
        coalesce(p_pix_key,'')
      )returning invite_code into first_code;
    else
      insert into public.events(
        group_id,title,location,starts_at,capacity,fee_cents,field_cost_cents,
        goalkeeper_exempt,pix_key
      )values(
        p_group,trim(p_title),trim(p_location),p_starts_at+n*interval '7 days',
        p_capacity,p_fee_cents,p_field_cost_cents,coalesce(p_keeper_exempt,true),
        coalesce(p_pix_key,'')
      );
    end if;
  end loop;
  return jsonb_build_object('code',first_code,'count',p_weeks);
end $$;

revoke all on function public.create_weekly_events(
  uuid,text,text,timestamptz,integer,integer,integer,boolean,text,integer
) from PUBLIC;
grant execute on function public.create_weekly_events(
  uuid,text,text,timestamptz,integer,integer,integer,boolean,text,integer
) to authenticated;
commit;
