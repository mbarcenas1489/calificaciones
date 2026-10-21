-- Corrige el error:
-- "new row violates row-level security policy for table Evaluations"
-- Ejecuta todo el archivo en Supabase > SQL Editor.

alter table public."Evaluations" enable row level security;
alter table public."EstudentEvalucation" enable row level security;

grant select, insert, update, delete
  on table public."Evaluations", public."EstudentEvalucation"
  to anon, authenticated;

grant usage, select on all sequences in schema public
  to anon, authenticated;

drop policy if exists "development evaluation access"
  on public."Evaluations";

create policy "development evaluation access"
  on public."Evaluations"
  for all
  to anon, authenticated
  using (true)
  with check (true);

drop policy if exists "development student evaluation access"
  on public."EstudentEvalucation";

create policy "development student evaluation access"
  on public."EstudentEvalucation"
  for all
  to anon, authenticated
  using (true)
  with check (true);

-- Verificación: deben aparecer ambas políticas después de ejecutar el script.
select schemaname, tablename, policyname, roles, cmd
from pg_policies
where schemaname = 'public'
  and tablename in ('Evaluations', 'EstudentEvalucation')
order by tablename, policyname;
