package org.regshield.ca.ui.screens.shield

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.regshield.ca.R
import org.regshield.ca.ui.components.CivicPrimaryButton
import org.regshield.ca.ui.components.CivicCard
import org.regshield.ca.ui.components.HeroPanel
import org.regshield.ca.ui.components.SplitColumns
import org.regshield.ca.ui.theme.LocalCivicAccent

private data class Flag(val id: String, val title: Int, val detail: Int)

private val FLAGS = listOf(
  Flag("pay", R.string.shield_flag_1_title, R.string.shield_flag_1_detail),
  Flag("rush", R.string.shield_flag_2_title, R.string.shield_flag_2_detail),
  Flag("no_bar", R.string.shield_flag_3_title, R.string.shield_flag_3_detail),
  Flag("docs", R.string.shield_flag_4_title, R.string.shield_flag_4_detail),
  Flag("offsite", R.string.shield_flag_5_title, R.string.shield_flag_5_detail),
)

@Composable
fun ShieldScreen(
  isCompact: Boolean,
  contentPadding: PaddingValues = PaddingValues(0.dp),
  onOpenVault: () -> Unit,
) {
  val checked = remember { mutableStateMapOf<String, Boolean>() }
  ShieldContent(
    isCompact = isCompact,
    contentPadding = contentPadding,
    checked = checked,
    onToggle = { id, value -> checked[id] = value },
    onOpenVault = onOpenVault,
  )
}

@Composable
fun ShieldContent(
  isCompact: Boolean,
  contentPadding: PaddingValues = PaddingValues(0.dp),
  checked: Map<String, Boolean>,
  onToggle: (String, Boolean) -> Unit,
  onOpenVault: () -> Unit,
) {
  val checkedCount = checked.values.count { it }
  val topPadding = contentPadding.calculateTopPadding()
  val bottomPadding = contentPadding.calculateBottomPadding()

  LazyColumn(
    modifier =
      Modifier
        .fillMaxSize()
        .imePadding(),
    contentPadding =
      PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = topPadding,
        bottom = bottomPadding,
      ),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      HeroPanel(
        title = stringResource(R.string.shield_title),
        subtitle = stringResource(R.string.shield_subtitle),
        badge = "${stringResource(R.string.nav_shield)} · $checkedCount",
      )
    }

    item {
      SplitColumns(
        isCompact = isCompact,
        modifier = Modifier.fillMaxWidth(),
        left = {
          CivicCard {
            Text(text = stringResource(R.string.shield_red_flags), style = MaterialTheme.typography.titleMedium)
            for ((index, flag) in FLAGS.withIndex()) {
              ShieldFlagTile(
                title = stringResource(flag.title),
                detail = stringResource(flag.detail),
                checked = checked[flag.id] == true,
                testTag = "shield_flag_$index",
                onChange = { onToggle(flag.id, it) },
              )
            }
          }
        },
        right = {
          CivicCard {
            Text(text = stringResource(R.string.shield_action_plan), style = MaterialTheme.typography.titleMedium)
            ActionStep(number = 1, text = stringResource(R.string.shield_step_verify))
            ActionStep(number = 2, text = stringResource(R.string.shield_step_receipts))
            ActionStep(number = 3, text = stringResource(R.string.shield_step_no_blank))
            CivicPrimaryButton(
              text = stringResource(R.string.shield_open_vault),
              icon = Icons.Default.Shield,
              onClick = onOpenVault,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .testTag("shield_open_vault"),
            )
          }
        },
      )
    }
  }
}

@Composable
private fun ShieldFlagTile(
  title: String,
  detail: String,
  checked: Boolean,
  testTag: String,
  onChange: (Boolean) -> Unit,
) {
  val accent = LocalCivicAccent.current
  val bg =
    if (checked) {
      accent.accentContainer.copy(alpha = 0.7f)
    } else {
      MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    }
  val border =
    if (checked) {
      accent.accent.copy(alpha = 0.55f)
    } else {
      MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    }

  Surface(
    modifier =
      Modifier
        .fillMaxWidth()
        .testTag(testTag)
        .clickable { onChange(!checked) },
    shape = MaterialTheme.shapes.extraLarge,
    color = bg,
    border = BorderStroke(width = 1.dp, color = border),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Checkbox(
        checked = checked,
        onCheckedChange = onChange,
        colors =
          CheckboxDefaults.colors(
            checkedColor = accent.accent,
            checkmarkColor = accent.onAccent,
          ),
      )
      Box(modifier = Modifier.weight(1f)) {
        androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text(text = detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}

@Composable
private fun ActionStep(number: Int, text: String) {
  val accent = LocalCivicAccent.current
  Surface(
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(
        shape = androidx.compose.foundation.shape.CircleShape,
        color = accent.accentContainer,
        contentColor = accent.onAccentContainer,
      ) {
        Box(
          modifier = Modifier.size(26.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(text = number.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
      }
      Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
  }
}
