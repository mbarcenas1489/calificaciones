-- Completa el modelo de evaluaciones para poder almacenar una nota por estudiante.
-- Ejecuta todo en Supabase > SQL Editor antes de usar la pantalla de notas.

alter table public."EstudentEvalucation"
  add column if not exists "Grade" numeric(8,2) null;

-- Solo puede existir una nota por estudiante y evaluación.
create unique index if not exists "EstudentEvalucation_student_evaluation_key"
  on public."EstudentEvalucation" ("IdStudent", "IdEvaluation");

alter table public."Evaluations" enable row level security;
alter table public."EstudentEvalucation" enable row level security;

grant select, insert, update, delete on table
  public."Evaluations", public."EstudentEvalucation" to anon, authenticated;
grant usage, select on all sequences in schema public to anon, authenticated;

drop policy if exists "development evaluation access" on public."Evaluations";
create policy "development evaluation access"
  on public."Evaluations" for all to anon, authenticated
  using (true) with check (true);

drop policy if exists "development student evaluation access" on public."EstudentEvalucation";
create policy "development student evaluation access"
  on public."EstudentEvalucation" for all to anon, authenticated
  using (true) with check (true);
