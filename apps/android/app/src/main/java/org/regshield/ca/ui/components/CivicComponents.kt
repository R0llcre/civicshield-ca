package org.regshield.ca.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.regshield.ca.ui.theme.LocalCivicAccent

@Composable
fun CivicBackground(modifier: Modifier = Modifier) {
  val colorScheme = MaterialTheme.colorScheme
  val accent = LocalCivicAccent.current
  val isDark = isSystemInDarkTheme()

  Box(modifier = modifier.fillMaxSize()) {
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              listOf(
                colorScheme.background,
                colorScheme.surface,
              ),
            ),
          )
          .drawWithCache {
            val gridStep = 56.dp.toPx()
            val gridStroke = 1.dp.toPx()
            val gridColor =
              colorScheme.onSurface.copy(alpha = if (isDark) 0.04f else 0.03f)

            val wash =
              Brush.linearGradient(
                colors =
                  listOf(
                    accent.accent.copy(alpha = if (isDark) 0.16f else 0.12f),
                    Color.Transparent,
                  ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height * 0.7f),
              )

            onDrawBehind {
              drawRect(wash)

              var x = 0f
              while (x <= size.width) {
                drawLine(
                  color = gridColor,
                  start = Offset(x, 0f),
                  end = Offset(x, size.height),
                  strokeWidth = gridStroke,
                )
                x += gridStep
              }
              var y = 0f
              while (y <= size.height) {
                drawLine(
                  color = gridColor,
                  start = Offset(0f, y),
                  end = Offset(size.width, y),
                  strokeWidth = gridStroke,
                )
                y += gridStep
              }
            }
          },
    )

    // Soft light blobs for depth (very low alpha; keep legible in dark theme).
    Box(
      Modifier
        .padding(top = 0.dp, start = 0.dp)
        .graphicsLayer(alpha = if (isDark) 0.18f else 0.26f)
        .clip(CircleShape)
        .background(Brush.radialGradient(listOf(accent.accent.copy(alpha = 0.55f), Color.Transparent)))
        .align(Alignment.TopStart)
        .fillMaxSize(0.52f),
    )
    Box(
      Modifier
        .padding(end = 0.dp, top = 30.dp)
        .graphicsLayer(alpha = if (isDark) 0.14f else 0.22f)
        .clip(CircleShape)
        .background(Brush.radialGradient(listOf(accent.accentContainer.copy(alpha = 0.75f), Color.Transparent)))
        .align(Alignment.TopEnd)
        .fillMaxSize(0.46f),
    )
    Box(
      Modifier
        .padding(bottom = 18.dp)
        .graphicsLayer(alpha = if (isDark) 0.12f else 0.2f)
        .clip(CircleShape)
        .background(Brush.radialGradient(listOf(accent.accent.copy(alpha = 0.22f), Color.Transparent)))
        .align(Alignment.BottomCenter)
        .fillMaxSize(0.42f),
    )
  }
}

@Composable
fun CivicCard(
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(14.dp),
  content: @Composable ColumnScope.() -> Unit,
) {
  Card(
    // Default to full-width cards for consistent scanning on mobile; callers can override via modifier.
    modifier = Modifier.fillMaxWidth().then(modifier),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    shape = MaterialTheme.shapes.large,
  ) {
    Column(
      modifier = Modifier.padding(contentPadding),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      content = content,
    )
  }
}

@Composable
fun HeroPanel(
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier,
  badge: String? = null,
  icon: ImageVector? = null,
) {
  val accent = LocalCivicAccent.current
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLarge,
    tonalElevation = 1.dp,
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
  ) {
    Row(modifier = Modifier.fillMaxWidth()) {
      Box(
        modifier =
          Modifier
            .padding(start = 10.dp, top = 10.dp, bottom = 10.dp)
            .clip(RoundedCornerShape(999.dp))
            .width(6.dp)
            .fillMaxHeight()
            .background(
              Brush.verticalGradient(
                listOf(
                  accent.accent.copy(alpha = 0.95f),
                  accent.accent.copy(alpha = 0.35f),
                ),
              ),
            ),
      )
      Column(modifier = Modifier.padding(start = 12.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)) {
        if (!badge.isNullOrBlank()) {
          InfoBadge(text = badge)
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (icon != null) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = accent.accent,
            )
          }
          Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
          )
        }
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 4.dp),
        )
      }
    }
  }
}

@Composable
fun InfoBadge(text: String, modifier: Modifier = Modifier) {
  val accent = LocalCivicAccent.current
  Surface(
    modifier = modifier,
    shape = CircleShape,
    color = accent.accentContainer.copy(alpha = 0.9f),
    contentColor = accent.onAccentContainer,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
  val normalized = status.lowercase()
  val (bg, fg) = when (normalized) {
    "open" -> Color(0xFFE7F6F1) to Color(0xFF0A614E)
    "waitlist" -> Color(0xFFFFF0DF) to Color(0xFF9E5805)
    "closed" -> Color(0xFFF0F3F7) to Color(0xFF445065)
    else -> Color(0xFFF0F3F7) to Color(0xFF445065)
  }
  Surface(
    modifier = modifier,
    shape = CircleShape,
    color = bg,
    contentColor = fg,
  ) {
    Text(
      text = status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
    )
  }
}

@Composable
fun SplitColumns(
  isCompact: Boolean,
  modifier: Modifier = Modifier,
  left: @Composable BoxScope.() -> Unit,
  right: @Composable BoxScope.() -> Unit,
) {
  if (isCompact) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Box(Modifier.fillMaxWidth(), content = left)
      Box(Modifier.fillMaxWidth(), content = right)
    }
  } else {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
      Box(Modifier.weight(1f), content = left)
      Box(Modifier.weight(1f), content = right)
    }
  }
}
