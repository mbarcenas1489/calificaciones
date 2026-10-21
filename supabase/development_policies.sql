-- Políticas de desarrollo para la app sin inicio de sesión.
-- Ejecutar una sola vez en el SQL Editor de Supabase si el CRUD devuelve 401/403.
-- Antes de publicar, agrega autenticación y reemplaza estas políticas públicas.

alter table public."Component" enable row level security;
alter table public."Group" enable row level security;
alter table public."Estudent" enable row level security;

grant select, insert, update, delete on table
  public."Component", public."Group", public."Estudent" to anon;

grant usage, select on all sequences in schema public to anon;

drop policy if exists "development component access" on public."Component";
create policy "development component access"
  on public."Component" for all to anon
  using (true) with check (true);

drop policy if exists "development group access" on public."Group";
create policy "development group access"
  on public."Group" for all to anon
  using (true) with check (true);

drop policy if exists "development student access" on public."Estudent";
create policy "development student access"
  on public."Estudent" for all to anon
  using (true) with check (true);
