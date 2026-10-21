-- Inserta estudiantes por nombre en el grupo con Id = 2.
-- No modifica la estructura de la tabla ni vuelve a insertar nombres existentes.

do $$
declare
  target_group_id bigint := 2;
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
      ('BALLESTEROS HERNANDEZ EDGAR JOSUE'),
      ('BONILLA CARCAMO CARLOS ALFREDO'),
      ('BRAVO FRANCO FRANCISCO LEONARDO'),
      ('BUSTAMANTE MANZANAREZ NEFTALI FRANCISCO'),
      ('CERDA OSORIO EDUARDO ENRIQUE'),
      ('CISNEROS CHAVEZ ROGER ANTONIO'),
      ('CORTEZ GOMEZ SHELCY DAVILENIS'),
      ('CRAXMAN JACKSON SHANELY SHARINA'),
      ('DELGADO FLORES SHENIELKA SHEYDANIE'),
      ('ESCORCIA TERCERO GÉNESIS MARITZA'),
      ('ESPINOZA BONICHE RANDY JAVIER'),
      ('ESPINOZA PIZARRO EDUARDO CÉSAR'),
      ('ESTRADA PAREDES ANDRES MAURICIO'),
      ('GARCIA ACOSTA GRETCHEL LOIRETH'),
      ('HERNÁNDEZ RUEDA EYNER JOSUÉ'),
      ('IBARRA SOLIS YENDER JOSE'),
      ('LEON MAYORGA ENMANUEL DICARLOS'),
      ('MARTÍNEZ HERNÁNDEZ KEVIN EDUARDO'),
      ('MARTINEZ TINOCO JUNIOR EFRAIN'),
      ('MÉNDEZ AMADOR JOEYMIR ELIEL'),
      ('MENDEZ ZEAS EFREN ALEXANDRO'),
      ('MIGUEL FELICIANO BAUDILIO LUIS'),
      ('MONTES SEVILLA CHRISTHIAN EDUARDO'),
      ('NARVÁEZ JIMÉNEZ OMAR MARCEL'),
      ('PÉREZ CÁRCAMO ALLAM SAMIR'),
      ('QUINTANA ALTAMIRANO DONALD JOSE'),
      ('RAMIREZ PEREZ RIDER EDUARDO'),
      ('ROA REYES ANAHI LUCIA'),
      ('SALAZAR BLANCO JOSHUA FERNANDO'),
      ('TREMINIO CHAVARRIA ORLANDO ANTONIO'),
      ('VARGAS BRICEÑO BRYANT JOSUE'),
      ('VARGAS MARCIA JORGE EDUARDO'),
      ('VEGA CRUZ JOSHUA JASMANY')
  ) as students(student_name)
  where not exists (
    select 1
    from public."Estudent" existing_student
    where existing_student."GroupId" = target_group_id
      and upper(trim(existing_student."Name")) = upper(trim(student_name))
  );
end
$$;

select "Id", "Name", "GroupId"
from public."Estudent"
where "GroupId" = 2
order by "Name";
