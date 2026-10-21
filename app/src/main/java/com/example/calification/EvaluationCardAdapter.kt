package com.example.calification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.PopupMenu
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
    val graded = row.gradedStudents ?: 0
    val pending = row.pendingStudents ?: 0
    val total = graded + pending
    val percentage = if (total == 0) 0 else (graded * 100) / total
    binding.progressIndicator.setProgressCompat(percentage, false)
    binding.progressLabel.text = parent.context.getString(R.string.evaluation_progress, graded, total)
    binding.gradedLabel.text = parent.context.getString(R.string.students_graded, graded)
    binding.pendingLabel.text = parent.context.getString(R.string.students_pending, pending)
    binding.content.setOnClickListener { onOpen(row) }
    binding.openButton.setOnClickListener { onOpen(row) }
    binding.moreButton.setOnClickListener { anchor ->
      PopupMenu(parent.context, anchor).apply {
        inflate(R.menu.menu_evaluation_card)
        setOnMenuItemClickListener { item ->
          when (item.itemId) {
            R.id.action_edit_evaluation -> onEdit(row)
            R.id.action_delete_evaluation -> onDelete(row)
            else -> return@setOnMenuItemClickListener false
          }
          true
        }
      }.show()
    }
    return binding.root
  }
}
