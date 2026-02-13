package org.regshield.ca.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.regshield.ca.R
import org.regshield.ca.ui.theme.LocalCivicAccent

data class ChromeNavItem(
  val route: String,
  val label: String,
  val iconRes: Int,
)

@Composable
fun AppChrome(
  isCompact: Boolean,
  currentRoute: String?,
  items: List<ChromeNavItem>,
  showBottomBar: Boolean,
  onNavigate: (String) -> Unit,
  content: @Composable (PaddingValues) -> Unit,
) {
  val statusTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
  val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
  val topBarHeight = 38.dp
  val floatingBarHeight = if (isCompact) 72.dp else 60.dp
  val contentBottomPadding =
    if (showBottomBar) {
      floatingBarHeight + navBottomInset + 24.dp
    } else {
      navBottomInset + 16.dp
    }

  Box(modifier = Modifier.fillMaxSize()) {
    content(
      PaddingValues(
        top = statusTopInset + topBarHeight + 2.dp,
        bottom = contentBottomPadding,
      ),
    )

    CompactTopBar(
      modifier =
        Modifier
          .align(Alignment.TopCenter)
          .padding(top = statusTopInset + 4.dp),
    )

    if (showBottomBar) {
      FloatingBottomBar(
        items = items,
        currentRoute = currentRoute,
        isCompact = isCompact,
        onNavigate = onNavigate,
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = navBottomInset + 8.dp),
      )
    }
  }
}

@Composable
private fun CompactTopBar(modifier: Modifier = Modifier) {
  val accent = LocalCivicAccent.current
  GlassPillSurface(
    modifier = modifier.widthIn(max = 340.dp),
    tonalElevation = 2.dp,
    shadowElevation = 1.dp,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(accent.accent),
      )
      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 8.dp),
      )
    }
  }
}

@Composable
private fun FloatingBottomBar(
  items: List<ChromeNavItem>,
  currentRoute: String?,
  isCompact: Boolean,
  onNavigate: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val accent = LocalCivicAccent.current
  val fontScale = LocalDensity.current.fontScale
  val barHeight = if (isCompact) 72.dp else 60.dp

  GlassPillSurface(
    modifier = modifier.widthIn(max = 440.dp),
    tonalElevation = 5.dp,
    shadowElevation = 10.dp,
  ) {
    Row(
      modifier =
        Modifier
          .height(barHeight)
          .padding(horizontal = 8.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      items.forEach { item ->
        val selected = currentRoute == item.route
        val showLabel = selected && fontScale <= 1.2f
        val bgColor by
          animateColorAsState(
            targetValue = if (selected) accent.accentContainer else androidx.compose.ui.graphics.Color.Transparent,
            animationSpec = tween(durationMillis = 200),
            label = "navBg",
          )
        val fgColor by
          animateColorAsState(
            targetValue = if (selected) accent.onAccentContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(durationMillis = 200),
            label = "navFg",
          )
        val horizontalPadding by
          animateDpAsState(
            targetValue = if (selected) 14.dp else 10.dp,
            animationSpec = tween(durationMillis = 200),
            label = "navPad",
          )

        Surface(
          modifier =
            Modifier
              .weight(1f)
              .clip(CircleShape)
              .clickable { onNavigate(item.route) },
          shape = CircleShape,
          color = bgColor,
          contentColor = fgColor,
        ) {
          Row(
            modifier =
              Modifier
                .fillMaxHeight()
                .padding(horizontal = horizontalPadding, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              painter = painterResource(item.iconRes),
              contentDescription = item.label,
              tint = fgColor,
              modifier = Modifier.size(if (selected) 20.dp else 22.dp),
            )
            AnimatedVisibility(visible = showLabel) {
              Text(
                text = item.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun GlassPillSurface(
  modifier: Modifier = Modifier,
  tonalElevation: androidx.compose.ui.unit.Dp = 0.dp,
  shadowElevation: androidx.compose.ui.unit.Dp = 0.dp,
  content: @Composable () -> Unit,
) {
  val accent = LocalCivicAccent.current
  val scheme = MaterialTheme.colorScheme
  val isDark = isSystemInDarkTheme()

  val surfaceAlpha = if (isDark) 0.72f else 0.86f
  val highlightAlphaTop = if (isDark) 0.16f else 0.28f
  val highlightAlphaBottom = if (isDark) 0.06f else 0.12f
  val washAlpha = if (isDark) 0.10f else 0.12f

  val borderBrush =
    Brush.linearGradient(
      colors =
        listOf(
          Color.White.copy(alpha = highlightAlphaTop),
          Color.White.copy(alpha = highlightAlphaBottom),
          Color.Transparent,
        ),
      start = Offset.Zero,
      end = Offset(420f, 180f),
    )

  val washBrush =
    Brush.linearGradient(
      colors =
        listOf(
          accent.accent.copy(alpha = washAlpha),
          Color.Transparent,
        ),
      start = Offset.Zero,
      end = Offset(520f, 320f),
    )

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(999.dp),
    color = scheme.surface.copy(alpha = surfaceAlpha),
    contentColor = scheme.onSurface,
    tonalElevation = tonalElevation,
    shadowElevation = shadowElevation,
  ) {
    Box(
      modifier =
        Modifier
          .clip(RoundedCornerShape(999.dp))
          .border(BorderStroke(1.dp, borderBrush), RoundedCornerShape(999.dp))
          .background(washBrush),
    ) {
      content()
    }
  }
}
