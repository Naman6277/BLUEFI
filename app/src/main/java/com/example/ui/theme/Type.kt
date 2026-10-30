package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// 10 Distinct Working Font Styles loaded locally from res/font/
val InterFontFamily = FontFamily(Font(R.font.inter, FontWeight.Normal))
val RobotoMonoFontFamily = FontFamily(Font(R.font.roboto_mono, FontWeight.Normal))
val LexendFontFamily = FontFamily(Font(R.font.lexend, FontWeight.Normal))
val MontserratFontFamily = FontFamily(Font(R.font.montserrat, FontWeight.Normal))
val PoppinsFontFamily = FontFamily(Font(R.font.poppins, FontWeight.Normal))
val FiraCodeFontFamily = FontFamily(Font(R.font.fira_code, FontWeight.Normal))
val OpenSansFontFamily = FontFamily(Font(R.font.open_sans, FontWeight.Normal))
val LatoFontFamily = FontFamily(Font(R.font.lato, FontWeight.Normal))
val UbuntuFontFamily = FontFamily(Font(R.font.ubuntu, FontWeight.Normal))

enum class BluefiFontStyle(val key: String, val label: String, val fontFamily: FontFamily) {
  DEFAULT("DEFAULT", "System Default", FontFamily.Default),
  INTER("INTER", "Inter (Modern Geometric)", InterFontFamily),
  ROBOTO_MONO("ROBOTO_MONO", "Roboto Mono (Hacker Terminal)", RobotoMonoFontFamily),
  LEXEND("LEXEND", "Lexend (High Readability)", LexendFontFamily),
  MONTSERRAT("MONTSERRAT", "Montserrat (Clean Display)", MontserratFontFamily),
  POPPINS("POPPINS", "Poppins (Friendly Rounded)", PoppinsFontFamily),
  FIRA_CODE("FIRA_CODE", "Fira Code (Developer Ligatures)", FiraCodeFontFamily),
  OPEN_SANS("OPEN_SANS", "Open Sans (Neutral Balanced)", OpenSansFontFamily),
  LATO("LATO", "Lato (Warm Professional)", LatoFontFamily),
  UBUNTU("UBUNTU", "Ubuntu (Humanist Technical)", UbuntuFontFamily);

  companion object {
    fun fromKey(key: String): BluefiFontStyle =
      values().find { it.key.equals(key, ignoreCase = true) } ?: INTER
  }
}

fun createTypography(fontFamily: FontFamily): Typography {
  return Typography(
    bodyLarge = TextStyle(
      fontFamily = fontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 16.sp,
      lineHeight = 24.sp,
      letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
      fontFamily = fontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 14.sp,
      lineHeight = 20.sp,
      letterSpacing = 0.25.sp,
    ),
    titleMedium = TextStyle(
      fontFamily = fontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 16.sp,
      lineHeight = 24.sp,
      letterSpacing = 0.15.sp,
    ),
    titleLarge = TextStyle(
      fontFamily = fontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 20.sp,
      lineHeight = 28.sp,
      letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
      fontFamily = fontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 11.sp,
      lineHeight = 16.sp,
      letterSpacing = 0.5.sp
    )
  )
}

val Typography = createTypography(InterFontFamily)
