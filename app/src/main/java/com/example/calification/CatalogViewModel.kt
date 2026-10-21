package com.example.calification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CatalogScreen { COMPONENTS, GROUPS, STUDENTS, EVALUATIONS }

data class CatalogUiState(
  val screen: CatalogScreen = CatalogScreen.COMPONENTS,
  val selectedComponent: Component? = null,
  val selectedGroup: ClassGroup? = null,
  val components: List<Component> = emptyList(),
  val groups: List<ClassGroup> = emptyList(),
  val students: List<Student> = emptyList(),
  val evaluations: List<Evaluation> = emptyList(),
  val rows: List<EntityRow> = emptyList(),
  val loading: Boolean = true,
  val error: String? = null,
)

class CatalogViewModel(
  private val repository: SupabaseRepository = SupabaseRepository(),
) : ViewModel() {
  private val _state = MutableStateFlow(CatalogUiState())
  val state: StateFlow<CatalogUiState> = _state.asStateFlow()

  init {
    load()
  }

  fun load() {
    _state.value = _state.value.copy(loading = true, error = null)
    viewModelScope.launch {
      runCatching { loadCurrentScreen() }
        .onSuccess { _state.value = it.copy(loading = false) }
        .onFailure { _state.value = _state.value.copy(loading = false, rows = emptyList(), error = it.message ?: it.javaClass.simpleName) }
    }
  }

  private suspend fun loadCurrentScreen(): CatalogUiState {
    val current = _state.value
    return when (current.screen) {
      CatalogScreen.COMPONENTS -> {
        val items = repository.getComponents()
        current.copy(
          components = items,
          rows = items.map { EntityRow(it.id, it.name, "") },
        )
      }
      CatalogScreen.GROUPS -> {
        val componentId = requireNotNull(current.selectedComponent).id
        val items = repository.getGroups(componentId)
        current.copy(
          groups = items,
          rows = items.map { EntityRow(it.id, it.number.toString(), "") },
        )
      }
      CatalogScreen.STUDENTS -> {
        val groupId = requireNotNull(current.selectedGroup).id
        val items = repository.getStudents(groupId)
        current.copy(
          students = items,
          rows = items.map { EntityRow(it.id, it.name, "") },
        )
      }
      CatalogScreen.EVALUATIONS -> {
        val groupId = requireNotNull(current.selectedGroup).id
        val items = repository.getEvaluations(groupId)
        val groupStudents = repository.getStudents(groupId)
        val studentIds = groupStudents.map { it.id }.toSet()
        val rows = items.map { evaluation ->
          val gradedIds = repository.getEvaluationGrades(evaluation.id)
            .asSequence()
            .filter { it.grade != null }
            .mapNotNull { it.studentId }
            .filter { it in studentIds }
            .toSet()
          EntityRow(
            id = evaluation.id,
            title = evaluation.name,
            subtitle = "",
            maximumGrade = evaluation.value,
            gradedStudents = gradedIds.size,
            pendingStudents = (groupStudents.size - gradedIds.size).coerceAtLeast(0),
          )
        }
        current.copy(evaluations = items, students = groupStudents, rows = rows)
      }
    }
  }

  fun openRow(id: Int) {
    val current = _state.value
    _state.value = when (current.screen) {
      CatalogScreen.COMPONENTS -> current.copy(
        screen = CatalogScreen.GROUPS,
        selectedComponent = current.components.first { it.id == id },
        rows = emptyList(),
        loading = true,
      )
      CatalogScreen.GROUPS -> current.copy(
        screen = CatalogScreen.EVALUATIONS,
        selectedGroup = current.groups.first { it.id == id },
        rows = emptyList(),
        loading = true,
      )
      CatalogScreen.STUDENTS, CatalogScreen.EVALUATIONS -> current
    }
    if (_state.value !== current) load()
  }

  fun showStudents() {
    if (_state.value.selectedGroup == null || _state.value.screen == CatalogScreen.STUDENTS) return
    _state.value = _state.value.copy(screen = CatalogScreen.STUDENTS, rows = emptyList(), loading = true)
    load()
  }

  fun showEvaluations() {
    if (_state.value.selectedGroup == null || _state.value.screen == CatalogScreen.EVALUATIONS) return
    _state.value = _state.value.copy(screen = CatalogScreen.EVALUATIONS, rows = emptyList(), loading = true)
    load()
  }

  fun navigateBack(): Boolean {
    val current = _state.value
    _state.value = when (current.screen) {
      CatalogScreen.EVALUATIONS -> current.copy(screen = CatalogScreen.GROUPS, selectedGroup = null, rows = emptyList(), loading = true)
      CatalogScreen.STUDENTS -> current.copy(screen = CatalogScreen.EVALUATIONS, rows = emptyList(), loading = true)
      CatalogScreen.GROUPS -> current.copy(screen = CatalogScreen.COMPONENTS, selectedComponent = null, rows = emptyList(), loading = true)
      CatalogScreen.COMPONENTS -> return false
    }
    load()
    return true
  }

  fun component(id: Int): Component = state.value.components.first { it.id == id }
  fun group(id: Int): ClassGroup = state.value.groups.first { it.id == id }
  fun student(id: Int): Student = state.value.students.first { it.id == id }
  fun evaluation(id: Int): Evaluation = state.value.evaluations.first { it.id == id }

  fun saveComponent(component: Component?, name: String) = mutate {
    if (component == null) repository.createComponent(name) else repository.updateComponent(component.id, name)
  }

  fun saveGroup(group: ClassGroup?, number: Int) = mutate {
    val componentId = requireNotNull(state.value.selectedComponent).id
    if (group == null) repository.createGroup(componentId, number)
    else repository.updateGroup(group.id, componentId, number)
  }

  fun saveStudent(student: Student?, name: String) = mutate {
    val groupId = requireNotNull(state.value.selectedGroup).id
    if (student == null) repository.createStudent(groupId, name)
    else repository.updateStudent(student.id, groupId, name)
  }

  fun saveEvaluation(evaluation: Evaluation?, name: String, value: Int, description: String?) = mutate {
    val groupId = requireNotNull(state.value.selectedGroup).id
    if (evaluation == null) repository.createEvaluation(groupId, name, value, description)
    else repository.updateEvaluation(evaluation.id, groupId, name, value, description)
  }

  fun deleteRow(id: Int) = mutate {
    when (state.value.screen) {
      CatalogScreen.COMPONENTS -> repository.deleteComponent(id)
      CatalogScreen.GROUPS -> repository.deleteGroup(id)
      CatalogScreen.STUDENTS -> repository.deleteStudent(id)
      CatalogScreen.EVALUATIONS -> repository.deleteEvaluation(id)
    }
  }

  fun clearError() {
    _state.value = _state.value.copy(error = null)
  }

  private fun mutate(action: suspend () -> Unit) {
    _state.value = _state.value.copy(loading = true, error = null)
    viewModelScope.launch {
      runCatching { action() }
        .onSuccess { load() }
        .onFailure { _state.value = _state.value.copy(loading = false, error = it.message ?: it.javaClass.simpleName) }
    }
  }
}
