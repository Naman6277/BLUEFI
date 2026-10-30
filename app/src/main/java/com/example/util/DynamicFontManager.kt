package com.example.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.ui.theme.BluefiFontStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * DynamicFontManager:
 * Instant Runtime Font Switching (Zero Activity Recreate & Zero Socket/P2P Disconnect).
 *
 * 1. Maintains an in-memory application-wide StateFlow & Typeface singleton for the 10 Google Fonts.
 * 2. Emits immediate font changes directly to Compose Tree & View hierarchies.
 * 3. Keeps background P2P mesh sockets, Foreground Service, and socket listener threads 100% uninterrupted.
 */
object DynamicFontManager {
  private const val PREFS_NAME = "bluefi_theme_prefs"
  private const val KEY_FONT = "selected_app_font"

  private val typefaceCache = ConcurrentHashMap<BluefiFontStyle, Typeface?>()

  private val _activeFontState = MutableStateFlow(BluefiFontStyle.INTER)
  val activeFontState: StateFlow<BluefiFontStyle> = _activeFontState.asStateFlow()

  private var isListenerRegistered = false

  private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
    if (key == KEY_FONT) {
      val fontKey = prefs.getString(KEY_FONT, BluefiFontStyle.INTER.key) ?: BluefiFontStyle.INTER.key
      _activeFontState.value = BluefiFontStyle.fromKey(fontKey)
    }
  }

  fun initialize(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val savedFontKey = prefs.getString(KEY_FONT, BluefiFontStyle.INTER.key) ?: BluefiFontStyle.INTER.key
    _activeFontState.value = BluefiFontStyle.fromKey(savedFontKey)

    if (!isListenerRegistered) {
      prefs.registerOnSharedPreferenceChangeListener(prefsListener)
      isListenerRegistered = true
    }
  }

  fun getSelectedFont(context: Context): BluefiFontStyle {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val key = prefs.getString(KEY_FONT, _activeFontState.value.key) ?: _activeFontState.value.key
    return BluefiFontStyle.fromKey(key)
  }

  /**
   * Instantly changes active font at runtime:
   * 1. Updates persistent SharedPreferences.
   * 2. Updates activeFontState MutableStateFlow immediately in-memory.
   * 3. Seamlessly updates Compose typography and View hierarchies without recreation.
   */
  fun setFontInstant(context: Context, font: BluefiFontStyle) {
    _activeFontState.value = font
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_FONT, font.key).apply()
  }

  fun getTypeface(context: Context, font: BluefiFontStyle): Typeface? {
    return typefaceCache.getOrPut(font) {
      when (font) {
        BluefiFontStyle.DEFAULT -> Typeface.DEFAULT
        BluefiFontStyle.INTER -> ResourcesCompat.getFont(context, R.font.inter)
        BluefiFontStyle.ROBOTO_MONO -> ResourcesCompat.getFont(context, R.font.roboto_mono)
        BluefiFontStyle.LEXEND -> ResourcesCompat.getFont(context, R.font.lexend)
        BluefiFontStyle.MONTSERRAT -> ResourcesCompat.getFont(context, R.font.montserrat)
        BluefiFontStyle.POPPINS -> ResourcesCompat.getFont(context, R.font.poppins)
        BluefiFontStyle.FIRA_CODE -> ResourcesCompat.getFont(context, R.font.fira_code)
        BluefiFontStyle.OPEN_SANS -> ResourcesCompat.getFont(context, R.font.open_sans)
        BluefiFontStyle.LATO -> ResourcesCompat.getFont(context, R.font.lato)
        BluefiFontStyle.UBUNTU -> ResourcesCompat.getFont(context, R.font.ubuntu)
      }
    }
  }
}
