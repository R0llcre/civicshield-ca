package org.regshield.ca.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.regshield.ca.ui.theme.LocalCivicAccent

@Composable
fun CivicPrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  icon: ImageVector? = null,
) {
  val accent = LocalCivicAccent.current
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier =
      modifier
        .heightIn(min = 48.dp),
    shape = MaterialTheme.shapes.extraLarge,
    colors =
      ButtonDefaults.buttonColors(
        containerColor = accent.accent,
        contentColor = accent.onAccent,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
      ),
    contentPadding = ButtonDefaults.ContentPadding,
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (icon != null) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
      }
      Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
  }
}

@Composable
fun CivicTonalButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  icon: ImageVector? = null,
) {
  val accent = LocalCivicAccent.current
  FilledTonalButton(
    onClick = onClick,
    enabled = enabled,
    modifier =
      modifier
        .heightIn(min = 48.dp),
    shape = MaterialTheme.shapes.extraLarge,
    colors =
      ButtonDefaults.filledTonalButtonColors(
        containerColor = accent.accentContainer,
        contentColor = accent.onAccentContainer,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
      ),
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (icon != null) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
      }
      Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
  }
}

@Composable
fun CivicLinkButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  icon: ImageVector? = null,
) {
  val accent = LocalCivicAccent.current
  TextButton(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier,
    contentPadding = ButtonDefaults.TextButtonContentPadding,
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (icon != null) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = accent.accent,
        modifier = Modifier.padding(vertical = 2.dp),
      )
    }
  }
}
