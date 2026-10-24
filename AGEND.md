# Calification

## Descripción del proyecto

Calification es una aplicación Android diseñada para facilitar el registro, la organización y el seguimiento de las calificaciones obtenidas por los estudiantes en diferentes evaluaciones.

La aplicación permite organizar la información académica por componentes y grupos, registrar estudiantes, crear evaluaciones y asignar una nota individual a cada estudiante. Los datos se almacenan en Supabase para que la información pueda mantenerse centralizada y actualizada.

## Objetivo general

Desarrollar una herramienta móvil sencilla y práctica que permita a los docentes administrar las evaluaciones y calificaciones de sus estudiantes de forma ordenada.

## Objetivos específicos

- Organizar los cursos por componentes académicos y grupos.
- Registrar y administrar los estudiantes de cada grupo.
- Crear evaluaciones con nombre, descripción y nota máxima.
- Registrar y modificar las calificaciones de cada estudiante.
- Identificar rápidamente a los estudiantes con nota y sin nota.
- Buscar estudiantes por nombre durante el registro de calificaciones.
- Conservar temporalmente los cambios pendientes cuando se interrumpe el proceso de registro.
- Sincronizar la información con una base de datos remota.

## Funcionalidades actuales

- Creación, edición y eliminación de componentes académicos.
- Creación, edición y eliminación de grupos.
- Creación, edición y eliminación de estudiantes.
- Creación, edición y eliminación de evaluaciones.
- Registro de notas por estudiante y evaluación.
- Validación de las notas según el valor máximo de la evaluación.
- Visualización del progreso de calificación de cada evaluación.
- Filtros para mostrar todos los estudiantes, los pendientes o los que ya tienen nota.
- Búsqueda de estudiantes por nombre.
- Guardado local de borradores de notas pendientes.
- Sincronización automática cuando se recupera la conexión a Internet.
- Selección del tema visual de la aplicación.

## Tecnologías utilizadas

- Kotlin.
- Android SDK.
- Material Design Components.
- View Binding.
- Arquitectura MVVM.
- StateFlow para la gestión del estado de la interfaz.
- Supabase como base de datos y servicio de persistencia.
- Ktor como cliente de comunicación.
- Kotlin Serialization para transformar los datos.

## Organización de la información

La aplicación maneja la siguiente relación:

1. Un componente académico contiene uno o varios grupos.
2. Cada grupo contiene estudiantes y evaluaciones.
3. Cada evaluación tiene un nombre, una descripción opcional y una nota máxima.
4. A cada estudiante se le puede asignar una calificación para cada evaluación.

## Flujo principal de uso

1. Crear o seleccionar un componente académico.
2. Crear o seleccionar uno de sus grupos.
3. Registrar los estudiantes del grupo.
4. Crear las evaluaciones correspondientes.
5. Abrir una evaluación y registrar las notas de los estudiantes.
6. Guardar las calificaciones y consultar el progreso de la evaluación.

## Mejoras futuras sugeridas

- Calcular automáticamente promedios por estudiante, grupo y componente.
- Generar reportes de calificaciones.
- Exportar resultados a PDF o a una hoja de cálculo.
- Agregar autenticación para docentes.
- Permitir configurar porcentajes o ponderaciones para las evaluaciones.
- Incorporar estadísticas y gráficas de rendimiento académico.
- Agregar copias de seguridad y recuperación de información.

