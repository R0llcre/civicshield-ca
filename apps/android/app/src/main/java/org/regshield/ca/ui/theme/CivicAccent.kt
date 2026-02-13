package org.regshield.ca.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class CivicAccentColors(
  val accent: Color,
  val onAccent: Color,
  val accentContainer: Color,
  val onAccentContainer: Color,
)

val LocalCivicAccent =
  compositionLocalOf {
    CivicAccentColors(
      accent = CivicBlue,
      onAccent = Color.White,
      accentContainer = CivicBlueSoft,
      onAccentContainer = CivicInk,
    )
  }

