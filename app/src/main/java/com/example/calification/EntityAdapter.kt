package com.example.calification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.example.calification.databinding.ItemEntityBinding
import com.example.calification.databinding.ItemStudentEvaluationBinding
import com.google.android.material.color.MaterialColors

data class StudentEvaluationGrade(
  val evaluationName: String,
  val maximumGrade: Int,
  val grade: Double?,
)

data class EntityRow(
  val id: Int,
  val title: String,
  val subtitle: String,
  val maximumGrade: Int? = null,
  val gradedStudents: Int? = null,
  val pendingStudents: Int? = null,
  val studentGrades: List<StudentEvaluationGrade>? = null,
)

class EntityAdapter(
  private var rows: List<EntityRow> = emptyList(),
  private val onOpen: (EntityRow) -> Unit,
  private val onEdit: (EntityRow) -> Unit,
  private val onDelete: (EntityRow) -> Unit,
) : BaseAdapter() {
  private var expandedStudentId: Int? = null

  fun submitList(newRows: List<EntityRow>) {
    rows = newRows
    if (newRows.none { it.id == expandedStudentId && it.studentGrades != null }) {
      expandedStudentId = null
    }
    notifyDataSetChanged()
  }

  override fun getCount(): Int = rows.size

  override fun getItem(position: Int): EntityRow = rows[position]

  override fun getItemId(position: Int): Long = rows[position].id.toLong()

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    val binding = if (convertView == null) {
      ItemEntityBinding.inflate(LayoutInflater.from(parent.context), parent, false).also {
        it.root.tag = it
      }
    } else {
      convertView.tag as ItemEntityBinding
    }
    val row = getItem(position)
    val isStudent = row.studentGrades != null
    val isSelectedStudent = isStudent && expandedStudentId == row.id
    binding.title.text = row.title
    binding.subtitle.text = row.subtitle
    binding.evaluations.removeAllViews()
    row.studentGrades?.forEach { evaluation ->
      val evaluationBinding = ItemStudentEvaluationBinding.inflate(
        LayoutInflater.from(parent.context),
        binding.evaluations,
        false,
      )
      evaluationBinding.evaluationName.text = evaluation.evaluationName
      evaluationBinding.evaluationGrade.text = evaluation.grade?.let {
        parent.context.getString(
          R.string.student_grade_value,
          formatGrade(it),
          evaluation.maximumGrade,
        )
      } ?: parent.context.getString(R.string.student_without_grade)
      evaluationBinding.evaluationGrade.setBackgroundResource(
        if (evaluation.grade != null) R.drawable.bg_grade_badge else R.drawable.bg_grade_badge_pending,
      )
      evaluationBinding.evaluationGrade.setTextColor(
        MaterialColors.getColor(
          evaluationBinding.evaluationGrade,
          if (evaluation.grade != null) {
            com.google.android.material.R.attr.colorOnPrimaryContainer
          } else {
            com.google.android.material.R.attr.colorOnSurfaceVariant
          },
        ),
      )
      binding.evaluations.addView(evaluationBinding.root)
    }
    binding.evaluations.visibility = if (row.studentGrades.isNullOrEmpty()) View.GONE else View.VISIBLE
    val showActions = !isStudent || isSelectedStudent
    binding.actions.visibility = if (showActions) View.VISIBLE else View.GONE
    binding.root.strokeColor = MaterialColors.getColor(
      binding.root,
      if (isSelectedStudent) {
        com.google.android.material.R.attr.colorPrimary
      } else {
        com.google.android.material.R.attr.colorOutlineVariant
      },
    )
    binding.root.strokeWidth = dpToPx(binding.root, if (isSelectedStudent) 2 else 1)
    binding.content.setOnClickListener {
      if (isStudent) {
        expandedStudentId = if (expandedStudentId == row.id) null else row.id
        notifyDataSetChanged()
      } else {
        onOpen(row)
      }
    }
    binding.editButton.setOnClickListener { onEdit(row) }
    binding.deleteButton.setOnClickListener { onDelete(row) }
    return binding.root
  }

  private fun formatGrade(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

  private fun dpToPx(view: View, value: Int): Int =
    (value * view.resources.displayMetrics.density).toInt()
}
