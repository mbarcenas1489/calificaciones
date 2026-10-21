package com.example.calification

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemePreferences {
  private const val PREFERENCES = "appearance_preferences"
  private const val KEY_THEME_MODE = "theme_mode"

  val modes = intArrayOf(
    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
    AppCompatDelegate.MODE_NIGHT_NO,
    AppCompatDelegate.MODE_NIGHT_YES,
  )

  fun apply(context: Context) {
    AppCompatDelegate.setDefaultNightMode(selectedMode(context))
  }

  fun selectedMode(context: Context): Int =
    context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
      .getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

  fun select(context: Context, mode: Int) {
    require(mode in modes)
    context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
      .edit()
      .putInt(KEY_THEME_MODE, mode)
      .apply()
    AppCompatDelegate.setDefaultNightMode(mode)
  }
}
