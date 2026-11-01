-- Hermes 차량 간 와이퍼 관측 공유 (공모전 시연용)
create table public.wiper_reports (
  id bigint generated always as identity primary key,
  vehicle_id uuid not null,
  lat double precision not null check (lat between 33 and 39.5),
  lon double precision not null check (lon between 124 and 132),
  -- RainLevel ordinal: 0 비 없음, 1 약한 비, 2 보통 비, 3 강한 비, 4 매우 강한 비
  level smallint not null check (level between 0 and 4),
  wiper text check (char_length(wiper) <= 32),
  demo boolean not null default false,
  observed_at timestamptz not null default now()
);

create index wiper_reports_observed_at_idx on public.wiper_reports (observed_at desc);

alter table public.wiper_reports enable row level security;

-- 앱(anon/publishable 키): 지금 시각의 실제 관측만 넣을 수 있다
create policy "insert current real reports" on public.wiper_reports
  for insert to anon
  with check (
    demo = false
    and observed_at between now() - interval '2 minutes' and now() + interval '1 minute'
  );

-- 최근 30분 관측만 읽을 수 있다
create policy "read recent reports" on public.wiper_reports
  for select to anon
  using (observed_at > now() - interval '30 minutes');

-- 시연용 가상 차량 관측: 두 지점 사이에 n대를 고르게 배치, 관측 시각은 0~8분 전.
-- 오래된 시연 데이터(10분 초과)는 함께 지운다.
create or replace function public.seed_demo_fleet(
  lat1 double precision, lon1 double precision,
  lat2 double precision, lon2 double precision,
  lvl smallint, n integer
) returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
  cnt integer := least(greatest(n, 1), 20);
  i integer;
  f double precision;
begin
  if lvl not between 0 and 4 then raise exception 'level out of range'; end if;
  delete from wiper_reports where demo and observed_at < now() - interval '10 minutes';
  for i in 0..cnt - 1 loop
    f := case when cnt = 1 then 0.5 else i::double precision / (cnt - 1) end;
    insert into wiper_reports (vehicle_id, lat, lon, level, wiper, demo, observed_at)
    values (
      gen_random_uuid(),
      lat1 + (lat2 - lat1) * f + (random() - 0.5) * 0.01,
      lon1 + (lon2 - lon1) * f + (random() - 0.5) * 0.01,
      lvl, 'DEMO', true,
      now() - (random() * interval '8 minutes')
    );
  end loop;
  return cnt;
end;
$$;

revoke all on function public.seed_demo_fleet(double precision, double precision, double precision, double precision, smallint, integer) from public;
grant execute on function public.seed_demo_fleet(double precision, double precision, double precision, double precision, smallint, integer) to anon;
