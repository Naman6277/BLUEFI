package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.R

// 4 Distinct Working App Themes
enum class BluefiThemeOption(
  val key: String,
  val label: String,
  val styleResId: Int,
  val bgColor: Color,
  val cardColor: Color,
  val accentColor: Color
) {
  DARK_FAST_BLUE(
    key = "CLASSIC_NAVY",
    label = "Classic Dark Navy Slate (Default)",
    styleResId = R.style.Theme_MyApplication_ClassicNavy,
    bgColor = NavySlateBackground,
    cardColor = NavySlateCards,
    accentColor = NavySlateAccent
  ),
  JET_BLACK_CYBER(
    key = "JET_BLACK",
    label = "Jet Black Cyber",
    styleResId = R.style.Theme_MyApplication_JetBlack,
    bgColor = JetBlackBackground,
    cardColor = JetBlackCards,
    accentColor = JetBlackAccent
  ),
  CERULEAN_BLUE(
    key = "CERULEAN",
    label = "Cerulean Blue",
    styleResId = R.style.Theme_MyApplication_Cerulean,
    bgColor = CeruleanBackground,
    cardColor = CeruleanCards,
    accentColor = CeruleanAccent
  ),
  EMERALD_MESH(
    key = "EMERALD",
    label = "Emerald Mesh (OLED)",
    styleResId = R.style.Theme_MyApplication_Emerald,
    bgColor = EmeraldBackground,
    cardColor = EmeraldCards,
    accentColor = EmeraldAccent
  );

  companion object {
    fun fromKey(key: String): BluefiThemeOption =
      values().find { it.key.equals(key, ignoreCase = true) || it.name.equals(key, ignoreCase = true) } ?: DARK_FAST_BLUE
  }
}

fun getDynamicColorScheme(theme: BluefiThemeOption): ColorScheme {
  return darkColorScheme(
    primary = theme.accentColor,
    onPrimary = Color.White,
    primaryContainer = theme.accentColor,
    onPrimaryContainer = Color.White,
    secondary = StatusCyan,
    onSecondary = Color.Black,
    secondaryContainer = theme.cardColor,
    onSecondaryContainer = Color.White,
    background = theme.bgColor,
    onBackground = IceWhite,
    surface = theme.cardColor,
    onSurface = IceWhite,
    surfaceVariant = theme.cardColor,
    onSurfaceVariant = MutedSteel,
    outline = CyberBorderOutline,
    outlineVariant = CyberBorderOutline,
    error = BurnWarningRed,
    onError = Color.White
  )
}

val StrictDarkFastBlueColorScheme = getDynamicColorScheme(BluefiThemeOption.DARK_FAST_BLUE)
val HackerColorScheme = getDynamicColorScheme(BluefiThemeOption.EMERALD_MESH)

@Composable
fun BluefiTheme(
  themeOption: BluefiThemeOption = BluefiThemeOption.DARK_FAST_BLUE,
  fontStyle: BluefiFontStyle = BluefiFontStyle.INTER,
  content: @Composable () -> Unit,
) {
  MyApplicationTheme(themeOption = themeOption, fontStyle = fontStyle, content = content)
}

@Composable
fun MyApplicationTheme(
  themeOption: BluefiThemeOption = BluefiThemeOption.DARK_FAST_BLUE,
  fontStyle: BluefiFontStyle = BluefiFontStyle.INTER,
  content: @Composable () -> Unit,
) {
  val colorScheme = getDynamicColorScheme(themeOption)
  val typography = createTypography(fontStyle.fontFamily)

  MaterialTheme(
    colorScheme = colorScheme,
    typography = typography,
    content = content
  )
}
