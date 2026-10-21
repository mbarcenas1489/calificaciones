package com.example.calification

import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.calification.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
  private lateinit var binding: ActivityMainBinding
  private val viewModel: CatalogViewModel by viewModels()

  private val listAdapter by lazy {
    EntityAdapter(
      onOpen = ::openRow,
      onEdit = ::editRow,
      onDelete = ::confirmDelete,
    )
  }
  private val evaluationAdapter by lazy {
    EvaluationCardAdapter(
      onOpen = ::openRow,
      onEdit = ::editRow,
      onDelete = ::confirmDelete,
    )
  }
  private val gradeEntryLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult(),
  ) { result ->
    if (result.resultCode == RESULT_OK) viewModel.load()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    ThemePreferences.apply(this)
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
      val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
      insets
    }
    setSupportActionBar(binding.toolbar)
    binding.entityList.adapter = listAdapter
    binding.evaluationGrid.adapter = evaluationAdapter
    binding.fab.setOnClickListener { showEditor() }
    binding.emptyAction.setOnClickListener { showEditor() }
    binding.groupNavigation.setOnItemSelectedListener { item ->
      when (item.itemId) {
        R.id.navigation_evaluations -> viewModel.showEvaluations()
        R.id.navigation_students -> viewModel.showStudents()
        else -> return@setOnItemSelectedListener false
      }
      true
    }
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        if (!viewModel.navigateBack()) {
          isEnabled = false
          onBackPressedDispatcher.onBackPressed()
        }
      }
    })
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.state.collect(::render)
      }
    }
  }

  override fun onSupportNavigateUp(): Boolean = viewModel.navigateBack() || super.onSupportNavigateUp()

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.action_theme -> {
        showThemeSelector()
        return true
      }
    }
    return super.onOptionsItemSelected(item)
  }

  private fun showThemeSelector() {
    val selected = ThemePreferences.modes.indexOf(ThemePreferences.selectedMode(this))
      .takeIf { it >= 0 } ?: 0
    MaterialAlertDialogBuilder(this)
      .setTitle(R.string.theme)
      .setSingleChoiceItems(R.array.theme_options, selected) { dialog, which ->
        dialog.dismiss()
        ThemePreferences.select(this, ThemePreferences.modes[which])
      }
      .setNegativeButton(R.string.cancel, null)
      .show()
  }

  private fun render(state: CatalogUiState) {
    invalidateOptionsMenu()
    supportActionBar?.setDisplayHomeAsUpEnabled(state.screen != CatalogScreen.COMPONENTS)
    supportActionBar?.title = when (state.screen) {
      CatalogScreen.COMPONENTS -> getString(R.string.components_title)
      CatalogScreen.GROUPS -> getString(R.string.groups_title, state.selectedComponent?.name.orEmpty())
      CatalogScreen.STUDENTS -> getString(R.string.students_tab)
      CatalogScreen.EVALUATIONS -> getString(R.string.evaluations_tab)
    }
    supportActionBar?.subtitle = when (state.screen) {
      CatalogScreen.COMPONENTS -> null
      CatalogScreen.GROUPS -> getString(R.string.route_component, state.selectedComponent?.name.orEmpty())
      CatalogScreen.STUDENTS, CatalogScreen.EVALUATIONS -> getString(
        R.string.route_group,
        state.selectedComponent?.name.orEmpty(),
        state.selectedGroup?.number ?: 0,
      )
    }
    binding.emptyLabel.setText(
      when (state.screen) {
        CatalogScreen.COMPONENTS -> R.string.no_components
        CatalogScreen.GROUPS -> R.string.no_groups
        CatalogScreen.STUDENTS -> R.string.no_students
        CatalogScreen.EVALUATIONS -> R.string.no_evaluations
      },
    )
    binding.progress.visibility = if (state.loading) View.VISIBLE else View.GONE
    binding.fab.isEnabled = !state.loading
    val inGroup = state.screen == CatalogScreen.STUDENTS || state.screen == CatalogScreen.EVALUATIONS
    binding.groupNavigation.visibility = if (inGroup) View.VISIBLE else View.GONE
    (binding.fab.layoutParams as CoordinatorLayout.LayoutParams).apply {
      bottomMargin = ((if (inGroup) 88 else 16) * resources.displayMetrics.density).toInt()
      binding.fab.layoutParams = this
    }
    if (inGroup) {
      binding.groupNavigation.selectedItemId = if (state.screen == CatalogScreen.EVALUATIONS) {
        R.id.navigation_evaluations
      } else {
        R.id.navigation_students
      }
    }
    val displayRows = localizedRows(state)
    val showContent = !state.loading && displayRows.isNotEmpty()
    val evaluations = state.screen == CatalogScreen.EVALUATIONS
    if (evaluations) evaluationAdapter.submitList(displayRows) else listAdapter.submitList(displayRows)
    binding.entityList.visibility = if (showContent && !evaluations) View.VISIBLE else View.GONE
    binding.evaluationGrid.visibility = if (showContent && evaluations) View.VISIBLE else View.GONE
    binding.emptyView.visibility = if (!state.loading && state.rows.isEmpty()) View.VISIBLE else View.GONE
    state.error?.let {
      Snackbar.make(binding.main, getString(R.string.operation_error, it), Snackbar.LENGTH_LONG)
        .setAction(R.string.retry) { viewModel.load() }
        .show()
      viewModel.clearError()
    }
  }

  private fun localizedRows(state: CatalogUiState): List<EntityRow> {
    if (state.rows.isEmpty()) return emptyList()
    return when (state.screen) {
      CatalogScreen.COMPONENTS -> state.components.map {
        EntityRow(it.id, it.name, getString(R.string.component_subtitle))
      }
      CatalogScreen.GROUPS -> state.groups.map {
        EntityRow(it.id, getString(R.string.group_label, it.number), getString(R.string.group_subtitle))
      }
      CatalogScreen.STUDENTS -> state.students.map {
        EntityRow(it.id, it.name, getString(R.string.student_subtitle))
      }
      CatalogScreen.EVALUATIONS -> state.rows
    }
  }

  private fun openRow(row: EntityRow) {
    if (viewModel.state.value.screen == CatalogScreen.EVALUATIONS) {
      val evaluation = viewModel.evaluation(row.id)
      val groupId = requireNotNull(viewModel.state.value.selectedGroup).id
      gradeEntryLauncher.launch(GradeEntryActivity.createIntent(this, evaluation, groupId))
    } else {
      viewModel.openRow(row.id)
    }
  }

  private fun editRow(row: EntityRow) {
    when (viewModel.state.value.screen) {
      CatalogScreen.COMPONENTS -> showEditor(viewModel.component(row.id))
      CatalogScreen.GROUPS -> showEditor(viewModel.group(row.id))
      CatalogScreen.STUDENTS -> showEditor(viewModel.student(row.id))
      CatalogScreen.EVALUATIONS -> showEvaluationEditor(viewModel.evaluation(row.id))
    }
  }

  private fun showEditor(entity: Any? = null) {
    val screen = viewModel.state.value.screen
    if (screen == CatalogScreen.EVALUATIONS) {
      showEvaluationEditor(entity as? Evaluation)
      return
    }
    val numeric = screen == CatalogScreen.GROUPS
    val inputLayout = TextInputLayout(this).apply {
      hint = getString(
        when (screen) {
          CatalogScreen.COMPONENTS -> R.string.component_name
          CatalogScreen.GROUPS -> R.string.group_number
          CatalogScreen.STUDENTS -> R.string.student_name
          CatalogScreen.EVALUATIONS -> error("Evaluations have a specialized editor")
        },
      )
      val padding = (24 * resources.displayMetrics.density).toInt()
      setPadding(padding, 12, padding, 0)
    }
    val input = TextInputEditText(inputLayout.context).apply {
      inputType = if (numeric) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
      setText(
        when (entity) {
          is Component -> entity.name
          is ClassGroup -> entity.number.toString()
          is Student -> entity.name
          else -> ""
        },
      )
      setSelection(text?.length ?: 0)
    }
    inputLayout.addView(input)
    val title = when (screen) {
      CatalogScreen.COMPONENTS -> if (entity == null) R.string.new_component else R.string.edit_component
      CatalogScreen.GROUPS -> if (entity == null) R.string.new_group else R.string.edit_group
      CatalogScreen.STUDENTS -> if (entity == null) R.string.new_student else R.string.edit_student
      CatalogScreen.EVALUATIONS -> error("Evaluations have a specialized editor")
    }
    val dialog = MaterialAlertDialogBuilder(this)
      .setTitle(title)
      .setView(inputLayout)
      .setNegativeButton(R.string.cancel, null)
      .setPositiveButton(R.string.save, null)
      .create()
    dialog.setOnShowListener {
      input.requestFocus()
      dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        val value = input.text?.toString()?.trim().orEmpty()
        val number = if (numeric) value.toIntOrNull() else null
        inputLayout.error = when {
          value.isBlank() -> getString(R.string.required_field)
          numeric && (number == null || number <= 0) -> getString(R.string.invalid_number)
          else -> null
        }
        if (inputLayout.error != null) return@setOnClickListener
        dialog.dismiss()
        when (screen) {
          CatalogScreen.COMPONENTS -> viewModel.saveComponent(entity as? Component, value)
          CatalogScreen.GROUPS -> viewModel.saveGroup(entity as? ClassGroup, requireNotNull(number))
          CatalogScreen.STUDENTS -> viewModel.saveStudent(entity as? Student, value)
          CatalogScreen.EVALUATIONS -> Unit
        }
      }
    }
    dialog.show()
  }

  private fun showEvaluationEditor(evaluation: Evaluation? = null) {
    val padding = (24 * resources.displayMetrics.density).toInt()
    val container = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      setPadding(padding, 8, padding, 0)
    }
    fun field(hint: Int, value: String, type: Int): Pair<TextInputLayout, TextInputEditText> {
      val layout = TextInputLayout(this).apply { this.hint = getString(hint) }
      val input = TextInputEditText(layout.context).apply { inputType = type; setText(value) }
      layout.addView(input)
      container.addView(layout)
      return layout to input
    }
    val (nameLayout, nameInput) = field(R.string.evaluation_name, evaluation?.name.orEmpty(), InputType.TYPE_CLASS_TEXT)
    val (valueLayout, valueInput) = field(R.string.evaluation_max_value, evaluation?.value?.toString().orEmpty(), InputType.TYPE_CLASS_NUMBER)
    val (_, descriptionInput) = field(R.string.evaluation_description, evaluation?.description.orEmpty(), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
    val dialog = MaterialAlertDialogBuilder(this)
      .setTitle(if (evaluation == null) R.string.new_evaluation else R.string.edit_evaluation)
      .setView(container)
      .setNegativeButton(R.string.cancel, null)
      .setPositiveButton(R.string.save, null)
      .create()
    dialog.setOnShowListener {
      dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        val name = nameInput.text?.toString()?.trim().orEmpty()
        val maximum = valueInput.text?.toString()?.toIntOrNull()
        nameLayout.error = if (name.isBlank()) getString(R.string.required_field) else null
        valueLayout.error = if (maximum == null || maximum <= 0 || maximum > Short.MAX_VALUE) getString(R.string.invalid_evaluation_value) else null
        if (nameLayout.error != null || valueLayout.error != null) return@setOnClickListener
        dialog.dismiss()
        viewModel.saveEvaluation(
          evaluation,
          name,
          requireNotNull(maximum),
          descriptionInput.text?.toString()?.trim()?.takeIf(String::isNotEmpty),
        )
      }
    }
    dialog.show()
  }

  private fun confirmDelete(row: EntityRow) {
    MaterialAlertDialogBuilder(this)
      .setTitle(R.string.confirm_delete_title)
      .setMessage(getString(R.string.confirm_delete_message, row.title))
      .setNegativeButton(R.string.cancel, null)
      .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteRow(row.id) }
      .show()
  }
}
