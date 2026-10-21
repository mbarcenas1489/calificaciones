package com.example.calification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.example.calification.databinding.ItemEvaluationCardBinding

class EvaluationCardAdapter(
  private var rows: List<EntityRow> = emptyList(),
  private val onOpen: (EntityRow) -> Unit,
  private val onEdit: (EntityRow) -> Unit,
  private val onDelete: (EntityRow) -> Unit,
) : BaseAdapter() {

  fun submitList(newRows: List<EntityRow>) {
    rows = newRows
    notifyDataSetChanged()
  }

  override fun getCount(): Int = rows.size

  override fun getItem(position: Int): EntityRow = rows[position]

  override fun getItemId(position: Int): Long = rows[position].id.toLong()

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    val binding = if (convertView == null) {
      ItemEvaluationCardBinding.inflate(LayoutInflater.from(parent.context), parent, false).also {
        it.root.tag = it
      }
    } else {
      convertView.tag as ItemEvaluationCardBinding
    }
    val row = getItem(position)
    binding.title.text = row.title
    binding.maximumGrade.text = parent.context.getString(R.string.maximum_grade, row.maximumGrade ?: 0)
    binding.gradedChip.text = parent.context.getString(R.string.students_graded, row.gradedStudents ?: 0)
    binding.pendingChip.text = parent.context.getString(R.string.students_pending, row.pendingStudents ?: 0)
    binding.content.setOnClickListener { onOpen(row) }
    binding.editButton.setOnClickListener { onEdit(row) }
    binding.deleteButton.setOnClickListener { onDelete(row) }
    return binding.root
  }
}
