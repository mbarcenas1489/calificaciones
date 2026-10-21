# Arquitectura MVVM

La aplicación separa sus responsabilidades en las siguientes capas:

## Model

- `Models.kt`: modelos de dominio y mapeo de columnas de Supabase.
- `EntityRow`: modelo de presentación utilizado por los adaptadores.

## Data

- `SupabaseRepository.kt`: único punto de acceso a Supabase. Contiene las consultas y operaciones CRUD.

## ViewModel

- `CatalogViewModel.kt`: estado y operaciones de componentes, grupos, estudiantes y evaluaciones.
- `GradeEntryViewModel.kt`: carga y persistencia de las notas de una evaluación.
- Los estados se exponen mediante `StateFlow`; no contienen referencias a `Activity`, `View` ni recursos Android.

## View

- `MainActivity.kt`: observa `CatalogUiState`, renderiza listas/tarjetas y presenta formularios.
- `GradeEntryActivity.kt`: observa `GradeEntryUiState` y administra únicamente los controles visuales de notas.
- `EntityAdapter.kt` y `EvaluationCardAdapter.kt`: renderizado de filas y tarjetas.

## Flujo de datos

1. La vista envía una intención al ViewModel.
2. El ViewModel ejecuta la operación mediante el repositorio.
3. El repositorio consulta o modifica Supabase.
4. El ViewModel publica un nuevo estado inmutable.
5. La vista observa el estado y vuelve a renderizarse.

La vista nunca consulta Supabase directamente y el repositorio no conoce las vistas.
