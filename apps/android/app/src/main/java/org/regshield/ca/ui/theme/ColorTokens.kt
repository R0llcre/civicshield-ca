package org.regshield.ca.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val CivicBlue = Color(0xFF0B4D91)
val CivicBlueSoft = Color(0xFFDCEBFA)
val CivicTeal = Color(0xFF0A6A53)
val CivicAmber = Color(0xFFA65A06)
val CivicInk = Color(0xFF0F172A)

val SuccessGreen = Color(0xFF0B7A63)
val WarningAmber = Color(0xFFAF6508)
val DangerRed = Color(0xFFB42318)

val LightBackground = Color(0xFFF4F8FC)
val LightBackgroundAlt = Color(0xFFEAF1FB)
val DarkBackground = Color(0xFF0B1324)
val DarkBackgroundAlt = Color(0xFF111B30)

val LightColorScheme = lightColorScheme(
  primary = CivicBlue,
  onPrimary = Color.White,
  primaryContainer = CivicBlueSoft,
  onPrimaryContainer = CivicInk,
  secondary = CivicTeal,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFD9F4EC),
  onSecondaryContainer = CivicInk,
  tertiary = CivicAmber,
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFFFFE7CC),
  onTertiaryContainer = CivicInk,
  background = LightBackground,
  onBackground = CivicInk,
  surface = Color(0xFFFCFDFF),
  onSurface = CivicInk,
  surfaceVariant = Color(0xFFF0F4FA),
  onSurfaceVariant = Color(0xFF3C475D),
  error = DangerRed,
  onError = Color.White,
)

val DarkColorScheme = darkColorScheme(
  primary = Color(0xFF8AB4F8),
  onPrimary = Color(0xFF062446),
  primaryContainer = Color(0xFF0D2E57),
  onPrimaryContainer = Color(0xFFD6E5FF),
  secondary = Color(0xFF66D0B4),
  onSecondary = Color(0xFF053B2E),
  secondaryContainer = Color(0xFF0C3A31),
  onSecondaryContainer = Color(0xFFB9F3E4),
  tertiary = Color(0xFFF7B267),
  onTertiary = Color(0xFF4F2A00),
  tertiaryContainer = Color(0xFF3D2200),
  onTertiaryContainer = Color(0xFFFFDCC0),
  background = DarkBackground,
  onBackground = Color(0xFFE3EAF7),
  surface = Color(0xFF111B30),
  onSurface = Color(0xFFE3EAF7),
  surfaceVariant = Color(0xFF1A2740),
  onSurfaceVariant = Color(0xFFC4D0E5),
  error = Color(0xFFFFB4AB),
  onError = Color(0xFF690005),
)
