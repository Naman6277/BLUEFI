package com.example.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.example.ui.theme.BluefiFontStyle
import com.example.ui.theme.BluefiThemeOption

/**
 * DynamicThemeManager: Manages 4 runtime application themes and 10 dynamic typography styles.
 * Enables runtime theme application via Activity.setTheme() before setContentView().
 */
object DynamicThemeManager {
  private const val PREFS_NAME = "bluefi_theme_prefs"
  private const val KEY_THEME = "selected_app_theme"
  private const val KEY_FONT = "selected_app_font"

  private fun getPrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  fun getSelectedTheme(context: Context): BluefiThemeOption {
    val key = getPrefs(context).getString(KEY_THEME, BluefiThemeOption.DARK_FAST_BLUE.key)
      ?: BluefiThemeOption.DARK_FAST_BLUE.key
    return BluefiThemeOption.fromKey(key)
  }

  fun setSelectedTheme(context: Context, theme: BluefiThemeOption) {
    getPrefs(context).edit().putString(KEY_THEME, theme.key).apply()
  }

  fun getSelectedFont(context: Context): BluefiFontStyle {
    val key = getPrefs(context).getString(KEY_FONT, BluefiFontStyle.INTER.key)
      ?: BluefiFontStyle.INTER.key
    return BluefiFontStyle.fromKey(key)
  }

  fun setSelectedFont(context: Context, font: BluefiFontStyle) {
    getPrefs(context).edit().putString(KEY_FONT, font.key).apply()
  }

  fun applyThemeToActivity(activity: Activity) {
    val theme = getSelectedTheme(activity)
    activity.setTheme(theme.styleResId)
  }
}
