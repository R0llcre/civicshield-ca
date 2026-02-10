package org.regshield.ca.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
  primary = Color(0xFF0F766E),
  onPrimary = Color.White,
  secondary = Color(0xFF155E75),
  onSecondary = Color.White,
  background = Color(0xFFF8FAFC),
  onBackground = Color(0xFF0B1220),
  surface = Color.White,
  onSurface = Color(0xFF0B1220),
)

private val DarkColors = darkColorScheme(
  primary = Color(0xFF14B8A6),
  onPrimary = Color(0xFF062925),
  secondary = Color(0xFF67E8F9),
  onSecondary = Color(0xFF06212A),
  background = Color(0xFF0B1220),
  onBackground = Color(0xFFE2E8F0),
  surface = Color(0xFF111827),
  onSurface = Color(0xFFE2E8F0),
)

@Composable
fun RegShieldTheme(
  darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColors else LightColors
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = androidx.compose.material3.Typography(),
    content = content,
  )
}

