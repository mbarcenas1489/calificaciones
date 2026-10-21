package com.example.calification

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.calification.databinding.ActivityGradeEntryBinding
import com.example.calification.databinding.ItemStudentGradeBinding
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

private data class GradeField(
  val student: Student,
  val binding: ItemStudentGradeBinding,
)

private enum class GradeFilter { ALL, PENDING, GRADED }

class GradeEntryActivity : AppCompatActivity() {
  private lateinit var binding: ActivityGradeEntryBinding
  private val viewModel: GradeEntryViewModel by viewModels()
  private val fields = mutableListOf<GradeField>()
  private var baselineValues = emptyMap<Int, String>()
  private var contentRendered = false
  private var hasUnsavedChanges = false
  private var currentFilter = GradeFilter.ALL
  private var restoredDraft = emptyMap<Int, String>()
  private var draftNoticeShown = false
  private var isSaving = false
  private var syncFailed = false
  private var autoSyncPending = false
  private var networkAvailable = false
  private var networkCallbackRegistered = false
  private var initialLoadFailed = false
  private lateinit var connectivityManager: ConnectivityManager
  private val networkCallback = object : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
      runOnUiThread {
        networkAvailable = true
        if (initialLoadFailed) viewModel.retry() else tryAutoSync()
      }
    }

    override fun onLost(network: Network) {
      runOnUiThread { networkAvailable = false }
    }
  }
  private val evaluationId by lazy { intent.getIntExtra(EXTRA_EVALUATION_ID, -1) }
  private val groupId by lazy { intent.getIntExtra(EXTRA_GROUP_ID, -1) }
  private val maxGrade by lazy { intent.getIntExtra(EXTRA_MAX_GRADE, 0) }
  private val evaluationName by lazy { intent.getStringExtra(EXTRA_EVALUATION_NAME).orEmpty() }

  override fun onCreate(savedInstanceState: Bundle?) {
    ThemePreferences.apply(this)
    super.onCreate(savedInstanceState)
    binding = ActivityGradeEntryBinding.inflate(layoutInflater)
    setContentView(binding.root)
    ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
      val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
      insets
    }
    binding.toolbar.title = getString(R.string.evaluation_grades_title, evaluationName)
    binding.toolbar.subtitle = getString(R.string.maximum_grade, maxGrade)
    binding.toolbar.setNavigationOnClickListener { attemptExit() }
    binding.saveButton.setOnClickListener { saveGrades() }
    binding.studentSearchInput.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
      override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = applyFilters()
      override fun afterTextChanged(text: Editable?) = Unit
    })
    binding.filterGroup.setOnCheckedStateChangeListener { _, checkedIds ->
      currentFilter = when (checkedIds.firstOrNull()) {
        R.id.filter_pending -> GradeFilter.PENDING
        R.id.filter_graded -> GradeFilter.GRADED
        else -> GradeFilter.ALL
      }
      applyFilters()
    }
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() = attemptExit()
    })
    if (evaluationId <= 0 || groupId <= 0 || maxGrade <= 0) {
      setLoading(false)
      Snackbar.make(binding.main, R.string.invalid_evaluation_data, Snackbar.LENGTH_LONG).show()
      return
    }
    restoredDraft = GradeDraftStore.load(this, evaluationId)
    autoSyncPending = restoredDraft.isNotEmpty()
    startNetworkMonitoring()
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.state.collect(::render)
      }
    }
    viewModel.configure(evaluationId, groupId, maxGrade)
  }

  override fun onDestroy() {
    if (networkCallbackRegistered) connectivityManager.unregisterNetworkCallback(networkCallback)
    super.onDestroy()
  }

  private fun render(state: GradeEntryUiState) {
    if (state.saved) {
      GradeDraftStore.clear(this, evaluationId)
      restoredDraft = emptyMap()
      autoSyncPending = false
      syncFailed = false
    } else if (state.saving) {
      syncFailed = false
    }
    isSaving = state.saving
    setLoading(state.loading)
    setSaving(state.saving)
    if (!state.loading && (!contentRendered || state.saved)) {
      showStudents(state.items, restoreScroll = contentRendered)
      contentRendered = true
    } else if (!state.loading && contentRendered) {
      applyFilters()
    }
    state.error?.let {
      initialLoadFailed = fields.isEmpty()
      if (fields.isNotEmpty()) {
        syncFailed = hasUnsavedChanges
        autoSyncPending = syncFailed
        updateGradeState()
      }
      showError(getString(R.string.operation_error, it))
      viewModel.clearError()
    }
    if (state.saved) {
      Snackbar.make(binding.main, R.string.grades_saved, Snackbar.LENGTH_SHORT).show()
      setResult(RESULT_OK)
      viewModel.clearSaved()
    }
  }

  private fun showStudents(items: List<StudentGradeItem>, restoreScroll: Boolean) {
    initialLoadFailed = false
    val previousScroll = if (restoreScroll) binding.scrollView.scrollY else 0
    fields.clear()
    binding.gradeContainer.removeAllViews()
    baselineValues = items.associate { item ->
      item.student.id to normalizeGrade(item.grade?.let(::formatGrade).orEmpty())
    }
    items.forEachIndexed { index, item ->
      val row = ItemStudentGradeBinding.inflate(layoutInflater, binding.gradeContainer, false)
      row.studentName.text = item.student.name
      row.studentAvatar.text = initials(item.student.name)
      row.gradeInput.filters = arrayOf(GradeRangeInputFilter(maxGrade.toDouble()))
      row.gradeInput.imeOptions = if (index == items.lastIndex) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT
      val serverValue = item.grade?.let(::formatGrade).orEmpty()
      row.gradeInput.setText(if (restoredDraft.containsKey(item.student.id)) restoredDraft[item.student.id] else serverValue)
      val field = GradeField(item.student, row)
      row.gradeInput.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(text: Editable?) {
          updateFieldColor(field)
          updateGradeState()
        }
      })
      updateFieldColor(field)
      fields += field
      binding.gradeContainer.addView(row.root)
    }
    hasUnsavedChanges = false
    updateGradeState()
    setLoading(false)
    applyFilters()
    if (restoreScroll) binding.scrollView.post { binding.scrollView.scrollTo(0, previousScroll) }
    if (restoredDraft.isNotEmpty() && !draftNoticeShown) {
      draftNoticeShown = true
      Snackbar.make(binding.main, R.string.draft_restored, Snackbar.LENGTH_LONG).show()
    }
    tryAutoSync()
  }

  private fun updateFieldColor(field: GradeField) {
    val grade = normalizeGrade(field.binding.gradeInput.text?.toString().orEmpty()).toDoubleOrNull()
    val entered = grade != null && grade in 0.0..maxGrade.toDouble()
    val green = ContextCompat.getColor(this, R.color.grade_entered)
    val normalStroke = MaterialColors.getColor(field.binding.gradeLayout, MaterialR.attr.colorOutline)
    field.binding.gradeLayout.boxStrokeColor = if (entered) green else normalStroke
  }

  private fun updateGradeState() {
    val pendingValues = pendingValues()
    hasUnsavedChanges = pendingValues.isNotEmpty()
    val graded = fields.count { normalizeGrade(it.binding.gradeInput.text?.toString().orEmpty()).isNotEmpty() }
    binding.gradeSummary.text = getString(R.string.grade_summary, fields.size, graded)
    binding.unsavedLabel.visibility = if (hasUnsavedChanges) View.VISIBLE else View.GONE
    if (hasUnsavedChanges) {
      GradeDraftStore.save(this, evaluationId, pendingValues)
    } else {
      GradeDraftStore.clear(this, evaluationId)
      autoSyncPending = false
      syncFailed = false
    }
    fields.forEach { updateSyncStatus(it) }
    binding.saveButton.isEnabled = !isSaving && hasUnsavedChanges && fields.isNotEmpty()
    applyFilters()
  }

  private fun updateSyncStatus(field: GradeField) {
    val value = normalizeGrade(field.binding.gradeInput.text?.toString().orEmpty())
    val changed = value != baselineValues[field.student.id].orEmpty()
    val status = when {
      changed && isSaving -> R.string.grade_status_syncing
      changed && syncFailed -> R.string.grade_status_offline
      changed -> R.string.grade_status_pending
      value.isNotEmpty() -> R.string.grade_status_saved
      else -> null
    }
    field.binding.gradeStatus.visibility = if (status == null) View.GONE else View.VISIBLE
    if (status != null) field.binding.gradeStatus.setText(status)
    field.binding.gradeStatus.setTextColor(
      ContextCompat.getColor(
        this,
        if (!changed && value.isNotEmpty()) R.color.grade_entered else R.color.pending_chip_text,
      ),
    )
  }

  private fun applyFilters() {
    if (!::binding.isInitialized) return
    val query = normalize(binding.studentSearchInput.text?.toString().orEmpty().trim())
    var visibleCount = 0
    fields.forEach { field ->
      val hasGrade = normalizeGrade(field.binding.gradeInput.text?.toString().orEmpty()).isNotEmpty()
      val matchesFilter = when (currentFilter) {
        GradeFilter.ALL -> true
        GradeFilter.PENDING -> !hasGrade
        GradeFilter.GRADED -> hasGrade
      }
      val matchesName = query.isEmpty() || normalize(field.student.name).contains(query)
      val visible = matchesFilter && matchesName
      field.binding.root.visibility = if (visible) View.VISIBLE else View.GONE
      if (visible) visibleCount++
    }
    val hasStudents = fields.isNotEmpty()
    binding.emptyView.setText(if (hasStudents) R.string.no_students_found else R.string.no_students_for_grades)
    binding.emptyView.visibility = if (!hasStudents || visibleCount == 0) View.VISIBLE else View.GONE
    binding.scrollView.visibility = if (hasStudents && visibleCount > 0) View.VISIBLE else View.GONE
  }

  private fun attemptExit() {
    if (!hasUnsavedChanges) {
      finish()
      return
    }
    MaterialAlertDialogBuilder(this)
      .setTitle(R.string.discard_changes_title)
      .setMessage(R.string.discard_changes_message)
      .setNegativeButton(R.string.cancel, null)
      .setPositiveButton(R.string.discard) { _, _ ->
        GradeDraftStore.clear(this, evaluationId)
        finish()
      }
      .show()
  }

  private fun normalize(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
      .replace(Regex("\\p{M}+"), "")
      .lowercase(Locale.getDefault())

  private fun normalizeGrade(value: String): String {
    val normalized = value.trim().replace(',', '.')
    return normalized.toDoubleOrNull()?.let(::formatGrade) ?: normalized
  }

  private fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter(String::isNotEmpty)
    return when {
      parts.isEmpty() -> "?"
      parts.size == 1 -> parts.first().take(2)
      else -> "${parts.first().first()}${parts.last().first()}"
    }.uppercase(Locale.getDefault())
  }

  private fun saveGrades() {
    if (!hasUnsavedChanges || isSaving) return
    autoSyncPending = true
    viewModel.save(pendingValues())
  }

  private fun currentValues(): Map<Int, String> =
    fields.associate { it.student.id to it.binding.gradeInput.text?.toString().orEmpty() }

  private fun pendingValues(): Map<Int, String> = currentValues().filter { (studentId, value) ->
    normalizeGrade(value) != baselineValues[studentId].orEmpty()
  }

  private fun setLoading(loading: Boolean) {
    binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
    if (loading) binding.scrollView.visibility = View.GONE
    binding.studentSearchLayout.isEnabled = !loading
    binding.filterAll.isEnabled = !loading
    binding.filterPending.isEnabled = !loading
    binding.filterGraded.isEnabled = !loading
    binding.saveButton.isEnabled = !loading && !isSaving && hasUnsavedChanges && fields.isNotEmpty()
  }

  private fun setSaving(saving: Boolean) {
    binding.saveProgress.visibility = if (saving) View.VISIBLE else View.GONE
    binding.saveButton.setText(if (saving) R.string.saving_grades else R.string.save_grades)
    binding.saveButton.isEnabled = !saving && hasUnsavedChanges && fields.isNotEmpty()
    fields.forEach {
      it.binding.gradeInput.isEnabled = !saving
      updateSyncStatus(it)
    }
  }

  private fun startNetworkMonitoring() {
    connectivityManager = getSystemService(ConnectivityManager::class.java)
    val activeNetwork = connectivityManager.activeNetwork
    networkAvailable = activeNetwork != null
    connectivityManager.registerDefaultNetworkCallback(networkCallback)
    networkCallbackRegistered = true
  }

  private fun tryAutoSync() {
    if (networkAvailable && autoSyncPending && hasUnsavedChanges && !isSaving && fields.isNotEmpty()) {
      saveGrades()
    }
  }

  private fun showError(message: String) {
    Snackbar.make(binding.main, message, Snackbar.LENGTH_LONG)
      .setAction(R.string.retry) {
        if (fields.isEmpty()) viewModel.retry() else saveGrades()
      }
      .show()
  }

  private fun formatGrade(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

  companion object {
    private const val EXTRA_EVALUATION_ID = "evaluation_id"
    private const val EXTRA_EVALUATION_NAME = "evaluation_name"
    private const val EXTRA_MAX_GRADE = "maximum_grade"
    private const val EXTRA_GROUP_ID = "group_id"

    fun createIntent(context: Context, evaluation: Evaluation, groupId: Int): Intent =
      Intent(context, GradeEntryActivity::class.java).apply {
        putExtra(EXTRA_EVALUATION_ID, evaluation.id)
        putExtra(EXTRA_EVALUATION_NAME, evaluation.name)
        putExtra(EXTRA_MAX_GRADE, evaluation.value)
        putExtra(EXTRA_GROUP_ID, groupId)
      }
  }
}

private class GradeRangeInputFilter(private val maximum: Double) : InputFilter {
  private val pattern = Regex("^\\d{0,5}([.,]\\d{0,2})?$")

  override fun filter(
    source: CharSequence,
    start: Int,
    end: Int,
    destination: Spanned,
    destinationStart: Int,
    destinationEnd: Int,
  ): CharSequence? {
    val proposed = buildString {
      append(destination.substring(0, destinationStart))
      append(source.subSequence(start, end))
      append(destination.substring(destinationEnd))
    }
    if (proposed.isEmpty()) return null
    if (!pattern.matches(proposed)) return ""
    val value = proposed.replace(',', '.').toDoubleOrNull() ?: return null
    return if (value in 0.0..maximum) null else ""
  }
}
