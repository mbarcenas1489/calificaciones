package com.example.calification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StudentGradeItem(
  val student: Student,
  val recordId: Int?,
  val grade: Double?,
)

data class GradeEntryUiState(
  val items: List<StudentGradeItem> = emptyList(),
  val loading: Boolean = true,
  val saved: Boolean = false,
  val error: String? = null,
)

class GradeEntryViewModel(
  private val repository: SupabaseRepository = SupabaseRepository(),
) : ViewModel() {
  private val _state = MutableStateFlow(GradeEntryUiState())
  val state: StateFlow<GradeEntryUiState> = _state.asStateFlow()
  private var configured = false
  private var evaluationId = -1
  private var groupId = -1
  private var maximum = 0

  fun configure(evaluationId: Int, groupId: Int, maximum: Int) {
    if (configured) return
    configured = true
    this.evaluationId = evaluationId
    this.groupId = groupId
    this.maximum = maximum
    load()
  }

  private fun load(saved: Boolean = false) {
    _state.value = _state.value.copy(loading = true, error = null)
    viewModelScope.launch {
      runCatching {
        val students = repository.getStudents(groupId)
        val gradesByStudent = repository.getEvaluationGrades(evaluationId).associateBy { it.studentId }
        students.map { student ->
          val existing = gradesByStudent[student.id]
          StudentGradeItem(student, existing?.id, existing?.grade)
        }
      }.onSuccess {
        _state.value = GradeEntryUiState(items = it, loading = false, saved = saved)
      }.onFailure {
        _state.value = _state.value.copy(loading = false, error = it.message ?: it.javaClass.simpleName)
      }
    }
  }

  fun save(values: Map<Int, String>) {
    _state.value = _state.value.copy(loading = true, error = null)
    viewModelScope.launch {
      runCatching {
        state.value.items.forEach { item ->
          val text = values[item.student.id]?.trim().orEmpty()
          val grade = text.replace(',', '.').toDoubleOrNull()
          if (text.isBlank() && item.recordId != null) {
            repository.deleteGrade(item.recordId)
          } else if (grade != null) {
            require(grade in 0.0..maximum.toDouble())
            repository.saveGrade(item.recordId, item.student.id, evaluationId, grade)
          }
        }
      }.onSuccess {
        load(saved = true)
      }.onFailure {
        _state.value = _state.value.copy(loading = false, error = it.message ?: it.javaClass.simpleName)
      }
    }
  }

  fun clearError() {
    _state.value = _state.value.copy(error = null)
  }

  fun clearSaved() {
    _state.value = _state.value.copy(saved = false)
  }
}
