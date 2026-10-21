package com.example.calification

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
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
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

private data class GradeField(
  val student: Student,
  val binding: ItemStudentGradeBinding,
)

class GradeEntryActivity : AppCompatActivity() {
  private lateinit var binding: ActivityGradeEntryBinding
  private val viewModel: GradeEntryViewModel by viewModels()
  private val fields = mutableListOf<GradeField>()
  private var contentRendered = false
  private val evaluationId by lazy { intent.getIntExtra(EXTRA_EVALUATION_ID, -1) }
  private val groupId by lazy { intent.getIntExtra(EXTRA_GROUP_ID, -1) }
  private val maxGrade by lazy { intent.getIntExtra(EXTRA_MAX_GRADE, 0) }
  private val evaluationName by lazy { intent.getStringExtra(EXTRA_EVALUATION_NAME).orEmpty() }

  override fun onCreate(savedInstanceState: Bundle?) {
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
    binding.toolbar.setNavigationOnClickListener { finish() }
    binding.saveButton.setOnClickListener { saveGrades() }
    if (evaluationId <= 0 || groupId <= 0 || maxGrade <= 0) {
      setLoading(false)
      showError(getString(R.string.invalid_evaluation_data))
      binding.saveButton.isEnabled = false
      return
    }
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.state.collect(::render)
      }
    }
    viewModel.configure(evaluationId, groupId, maxGrade)
  }

  private fun render(state: GradeEntryUiState) {
    setLoading(state.loading)
    if (!state.loading && !contentRendered) {
      showStudents(state.items)
      contentRendered = true
    }
    state.error?.let {
      showError(getString(R.string.operation_error, it))
      viewModel.clearError()
    }
    if (state.saved) {
      Toast.makeText(this, R.string.grades_saved, Toast.LENGTH_SHORT).show()
      setResult(RESULT_OK)
      finish()
    }
  }

  private fun showStudents(items: List<StudentGradeItem>) {
    fields.clear()
    binding.gradeContainer.removeAllViews()
    items.forEach { item ->
      val student = item.student
      val row = ItemStudentGradeBinding.inflate(layoutInflater, binding.gradeContainer, false)
      row.studentName.text = student.name
      row.gradeLayout.helperText = getString(R.string.maximum_short, maxGrade)
      row.gradeInput.filters = arrayOf(GradeRangeInputFilter(maxGrade.toDouble()))
      row.gradeInput.setText(item.grade?.let(::formatGrade).orEmpty())
      val field = GradeField(student, row)
      row.gradeInput.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(text: Editable?) = updateFieldColor(field)
      })
      updateFieldColor(field)
      fields += field
      binding.gradeContainer.addView(row.root)
    }
    setLoading(false)
    binding.emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    binding.scrollView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
    binding.saveButton.isEnabled = items.isNotEmpty()
  }

  private fun updateFieldColor(field: GradeField) {
    val text = field.binding.gradeInput.text?.toString()?.trim().orEmpty()
    val grade = text.replace(',', '.').toDoubleOrNull()
    val entered = grade != null && grade >= 0 && grade <= maxGrade
    val green = ContextCompat.getColor(this, R.color.grade_entered)
    val normalStroke = MaterialColors.getColor(field.binding.gradeLayout, MaterialR.attr.colorOutline)
    val greenBackground = ContextCompat.getColor(this, R.color.grade_entered_background)
    field.binding.gradeLayout.boxStrokeColor = if (entered) green else normalStroke
    field.binding.gradeLayout.boxBackgroundColor = if (entered) greenBackground else android.graphics.Color.TRANSPARENT
  }

  private fun saveGrades() {
    viewModel.save(fields.associate { it.student.id to it.binding.gradeInput.text?.toString().orEmpty() })
  }

  private fun setLoading(loading: Boolean) {
    binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
    binding.scrollView.visibility = if (loading) View.GONE else View.VISIBLE
    binding.saveButton.isEnabled = !loading
  }

  private fun showError(message: String) {
    Snackbar.make(binding.main, message, Snackbar.LENGTH_LONG).show()
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
