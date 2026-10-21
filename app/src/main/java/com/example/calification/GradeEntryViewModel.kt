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
  val saving: Boolean = false,
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

  private fun load(saved: Boolean = false, showLoading: Boolean = true) {
    _state.value = _state.value.copy(loading = showLoading, error = null)
    viewModelScope.launch {
      runCatching {
        val students = repository.getStudents(groupId)
        val gradesByStudent = repository.getEvaluationGrades(evaluationId).associateBy { it.studentId }
        students.map { student ->
          val existing = gradesByStudent[student.id]
          StudentGradeItem(student, existing?.id, existing?.grade)
        }
      }.onSuccess {
        _state.value = GradeEntryUiState(items = it, loading = false, saving = false, saved = saved)
      }.onFailure {
        _state.value = _state.value.copy(
          loading = false,
          saving = false,
          error = it.message ?: it.javaClass.simpleName,
        )
      }
    }
  }

  fun save(values: Map<Int, String>) {
    _state.value = _state.value.copy(saving = true, saved = false, error = null)
    viewModelScope.launch {
      runCatching {
        val gradesByStudent = mutableMapOf<Int, Double>()
        val recordIdsToDelete = mutableListOf<Int>()
        val itemsByStudent = state.value.items.associateBy { it.student.id }
        values.forEach { (studentId, rawValue) ->
          val item = itemsByStudent[studentId] ?: return@forEach
          val text = rawValue.trim()
          val grade = text.replace(',', '.').toDoubleOrNull()
          if (text.isBlank() && item.recordId != null) {
            recordIdsToDelete += item.recordId
          } else if (grade != null) {
            require(grade in 0.0..maximum.toDouble())
            gradesByStudent[studentId] = grade
          }
        }
        repository.saveGrades(evaluationId, gradesByStudent, recordIdsToDelete)
      }.onSuccess {
        load(saved = true, showLoading = false)
      }.onFailure {
        _state.value = _state.value.copy(
          loading = false,
          saving = false,
          error = it.message ?: it.javaClass.simpleName,
        )
      }
    }
  }

  fun clearError() {
    _state.value = _state.value.copy(error = null)
  }

  fun clearSaved() {
    _state.value = _state.value.copy(saved = false)
  }

  fun retry() {
    load(showLoading = state.value.items.isEmpty())
  }
}
