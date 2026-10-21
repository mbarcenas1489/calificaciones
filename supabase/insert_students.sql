-- Inserta estudiantes en un grupo existente.
-- Grupo de destino: Id = 2.
-- Ejecuta todo el script en Supabase > SQL Editor.
-- El script no vuelve a insertar un estudiante si ya existe en ese grupo.

-- Corrige la llave foránea: GroupId debe apuntar a Group, no a Component.
alter table public."Estudent"
  drop constraint if exists "Estudent_ComponentId_fkey";

alter table public."Estudent"
  drop constraint if exists "Estudent_GroupId_fkey";

alter table public."Estudent"
  add constraint "Estudent_GroupId_fkey"
  foreign key ("GroupId")
  references public."Group" ("Id");

do $$
declare
  target_group_id integer := 2;
begin
  if not exists (
    select 1
    from public."Group"
    where "Id" = target_group_id
  ) then
    raise exception 'No existe un grupo con Id = %', target_group_id;
  end if;

  insert into public."Estudent" ("Name", "GroupId")
  select student_name, target_group_id
  from (
    values
      ('ALVARADO AVENDAÑO JONATHAN JOEL'),
      ('ALVAREZ OBANDO HILARY VALESKA'),
      ('ARTEAGA VILLALOBOS JORVIN LEONEL'),
      ('BARRERA PICHARDO JEANNY ELIZABETH'),
      ('BERRIOS GAMBOA FABIOLA VANESSA'),
      ('CASTRO RÍOS EVANS ONEALL'),
      ('JIMENEZ FERRUFINO FRANCISCO JAVIER'),
      ('JIRÓN RUIZ WILLIAM'),
      ('LAGUNA RIVERA MISAEL ISAAC'),
      ('LÓPEZ DUARTE ETHAN DAVID ERNESTO'),
      ('MENDIETA MUÑOZ DEHUBRY VLADIMIR'),
      ('MENDOZA HERRERA GERALD ALEXANDER'),
      ('MORALES AGUILAR LUIS FERNANDO'),
      ('OLIVARES RUÍZ MARÍA GABRIELA'),
      ('PICHARDO REYES DIEGO FERNANDO'),
      ('QUIROZ SEVILLA DAYANNA FRANCELA'),
      ('REYES HERNANDEZ ANDROW STEVEEN'),
      ('SINCLAIR FLORES JURGEN ALLEN'),
      ('TORRES FLETES STEVEN ENMANUEL'),
      ('TÓRREZ STEVEN RICARDO'),
      ('URCUYO DUARTE CARLOS ELIAS'),
      ('URRUTIA CUEVAS ESTIPHEN YAHIR'),
      ('VALLE SALINAS JOSHUA ALEJANDRO'),
      ('ZAPATA PÉREZ CRISTOPHER DAVID')
  ) as students(student_name)
  where not exists (
    select 1
    from public."Estudent" existing_student
    where existing_student."GroupId" = target_group_id
      and upper(trim(existing_student."Name")) = upper(trim(student_name))
  );
end
$$;

-- Muestra la lista final del grupo para verificar el resultado.
select "Id", "Name", "GroupId"
from public."Estudent"
where "GroupId" = 2
order by "Name";
