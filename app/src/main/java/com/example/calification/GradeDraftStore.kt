package com.example.calification

import android.content.Context

object GradeDraftStore {
  // v2 intentionally ignores drafts written while repeated row ids could
  // duplicate one student's grade across the complete list on rotation.
  private const val PREFERENCES = "grade_drafts_v2"
  private const val SEPARATOR = '\u001F'

  fun load(context: Context, evaluationId: Int): Map<Int, String> =
    context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
      .getStringSet(key(evaluationId), emptySet())
      .orEmpty()
      .mapNotNull { entry ->
        val separatorIndex = entry.indexOf(SEPARATOR)
        if (separatorIndex < 1) return@mapNotNull null
        val studentId = entry.substring(0, separatorIndex).toIntOrNull() ?: return@mapNotNull null
        studentId to entry.substring(separatorIndex + 1)
      }
      .toMap()

  fun save(context: Context, evaluationId: Int, values: Map<Int, String>) {
    val encoded = values.mapTo(mutableSetOf()) { (studentId, value) ->
      "$studentId$SEPARATOR$value"
    }
    context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
      .edit()
      .putStringSet(key(evaluationId), encoded)
      .apply()
  }

  fun clear(context: Context, evaluationId: Int) {
    context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
      .edit()
      .remove(key(evaluationId))
      .apply()
  }

  private fun key(evaluationId: Int) = "evaluation_$evaluationId"
}
