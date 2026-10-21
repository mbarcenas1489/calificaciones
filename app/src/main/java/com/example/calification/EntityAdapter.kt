package com.example.calification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.example.calification.databinding.ItemEntityBinding

data class EntityRow(
  val id: Int,
  val title: String,
  val subtitle: String,
  val maximumGrade: Int? = null,
  val gradedStudents: Int? = null,
  val pendingStudents: Int? = null,
)

class EntityAdapter(
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
      ItemEntityBinding.inflate(LayoutInflater.from(parent.context), parent, false).also {
        it.root.tag = it
      }
    } else {
      convertView.tag as ItemEntityBinding
    }
    val row = getItem(position)
    binding.title.text = row.title
    binding.subtitle.text = row.subtitle
    binding.content.setOnClickListener { onOpen(row) }
    binding.editButton.setOnClickListener { onEdit(row) }
    binding.deleteButton.setOnClickListener { onDelete(row) }
    return binding.root
  }
}
