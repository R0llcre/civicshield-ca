package org.regshield.ca.ui.screens.shield

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.regshield.ca.R
import org.regshield.ca.ui.components.CivicPrimaryButton
import org.regshield.ca.ui.components.CivicLinkButton
import org.regshield.ca.ui.components.CivicListItem
import org.regshield.ca.ui.components.CivicTonalButton
import org.regshield.ca.ui.components.CivicCard
import org.regshield.ca.ui.components.HeroPanel
import org.regshield.ca.ui.components.InfoBadge
import org.regshield.ca.ui.components.SplitColumns
import org.regshield.ca.ui.theme.LocalCivicAccent
import androidx.hilt.navigation.compose.hiltViewModel

private data class Flag(val id: String, val title: Int, val detail: Int)
private data class PlanStep(val id: String, val text: Int)

private val FLAGS = listOf(
  Flag("pay", R.string.shield_flag_1_title, R.string.shield_flag_1_detail),
  Flag("rush", R.string.shield_flag_2_title, R.string.shield_flag_2_detail),
  Flag("no_bar", R.string.shield_flag_3_title, R.string.shield_flag_3_detail),
  Flag("docs", R.string.shield_flag_4_title, R.string.shield_flag_4_detail),
  Flag("offsite", R.string.shield_flag_5_title, R.string.shield_flag_5_detail),
)

private val STEPS = listOf(
  PlanStep("verify", R.string.shield_step_verify),
  PlanStep("receipts", R.string.shield_step_receipts),
  PlanStep("no_blank", R.string.shield_step_no_blank),
)

@Composable
fun ShieldScreen(
  isCompact: Boolean,
  contentPadding: PaddingValues = PaddingValues(0.dp),
  onOpenVault: () -> Unit,
  onGoVerify: () -> Unit,
  onGoHelp: () -> Unit,
  vm: ShieldViewModel = hiltViewModel(),
) {
  val ctx = LocalContext.current
  val st by vm.state.collectAsState()

  val selectedFlagsText =
    FLAGS
      .filter { st.selectedFlagIds.contains(it.id) }
      .map { ctx.getString(it.title) }
  val stepsText =
    STEPS.map { step ->
      ctx.getString(step.text) to st.completedStepIds.contains(step.id)
    }
  val summaryText =
    buildShieldSummary(
      appName = ctx.getString(R.string.app_name),
      selectedFlags = selectedFlagsText,
      steps = stepsText,
      disclaimer = ctx.getString(R.string.shield_info_only),
    )

  ShieldContent(
    isCompact = isCompact,
    contentPadding = contentPadding,
    selectedFlagIds = st.selectedFlagIds,
    completedStepIds = st.completedStepIds,
    onToggleFlag = { id, value -> vm.setFlagSelected(id, value) },
    onToggleStep = { id, value -> vm.setStepCompleted(id, value) },
    onClearAll = vm::clearAll,
    onCopySummary = { copyToClipboard(ctx, summaryText) },
    onShareSummary = { shareText(ctx, summaryText) },
    onOpenVault = onOpenVault,
    onGoVerify = onGoVerify,
    onGoHelp = onGoHelp,
  )
}

@Composable
fun ShieldContent(
  isCompact: Boolean,
  contentPadding: PaddingValues = PaddingValues(0.dp),
  selectedFlagIds: Set<String>,
  completedStepIds: Set<String>,
  onToggleFlag: (String, Boolean) -> Unit,
  onToggleStep: (String, Boolean) -> Unit,
  onClearAll: () -> Unit,
  onCopySummary: () -> Unit,
  onShareSummary: () -> Unit,
  onOpenVault: () -> Unit,
  onGoVerify: () -> Unit,
  onGoHelp: () -> Unit,
) {
  val checkedCount = selectedFlagIds.size
  val stepsDone = completedStepIds.size
  val riskRes =
    when {
      checkedCount == 0 -> R.string.shield_risk_low
      checkedCount <= 2 -> R.string.shield_risk_medium
      else -> R.string.shield_risk_high
    }
  var flagsExpanded by rememberSaveable { mutableStateOf(true) }
  var planExpanded by rememberSaveable { mutableStateOf(true) }
  val chromeTopPadding = contentPadding.calculateTopPadding()
  val chromeBottomPadding = contentPadding.calculateBottomPadding()

  LazyColumn(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(top = chromeTopPadding)
        .imePadding()
        .testTag("shield_scroll"),
    contentPadding =
      PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = 0.dp,
        bottom = chromeBottomPadding,
      ),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      HeroPanel(
        title = stringResource(R.string.shield_title),
        subtitle = stringResource(R.string.shield_subtitle),
        badge =
          stringResource(
            R.string.shield_badge,
            checkedCount,
            FLAGS.size,
            stepsDone,
            STEPS.size,
            stringResource(riskRes),
          ),
      )
    }

    item {
      if (isCompact) {
        RedFlagsCard(
          selectedFlagIds = selectedFlagIds,
          completedStepIds = completedStepIds,
          onToggleFlag = onToggleFlag,
          onClearAll = onClearAll,
          expanded = flagsExpanded,
          onToggleExpanded = { flagsExpanded = !flagsExpanded },
        )
      } else {
        SplitColumns(
          isCompact = false,
          modifier = Modifier.fillMaxWidth(),
          left = {
            RedFlagsCard(
              selectedFlagIds = selectedFlagIds,
              completedStepIds = completedStepIds,
              onToggleFlag = onToggleFlag,
              onClearAll = onClearAll,
              expanded = flagsExpanded,
              onToggleExpanded = { flagsExpanded = !flagsExpanded },
            )
          },
          right = {
            ActionPlanCard(
              completedStepIds = completedStepIds,
              onToggleStep = onToggleStep,
              onCopySummary = onCopySummary,
              onShareSummary = onShareSummary,
              expanded = planExpanded,
              onToggleExpanded = { planExpanded = !planExpanded },
            )
          },
        )
      }
    }

    if (isCompact) {
      item {
        ActionPlanCard(
          completedStepIds = completedStepIds,
          onToggleStep = onToggleStep,
          onCopySummary = onCopySummary,
          onShareSummary = onShareSummary,
          expanded = planExpanded,
          onToggleExpanded = { planExpanded = !planExpanded },
        )
      }
    }

    item {
      EvidenceVaultCard(onOpenVault = onOpenVault)
    }

    item {
      QuickNextActionsCard(onGoVerify = onGoVerify, onGoHelp = onGoHelp)
    }

    item {
      CivicCard {
        Text(
          text = stringResource(R.string.shield_info_only),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun RedFlagsCard(
  selectedFlagIds: Set<String>,
  completedStepIds: Set<String>,
  onToggleFlag: (String, Boolean) -> Unit,
  onClearAll: () -> Unit,
  expanded: Boolean,
  onToggleExpanded: () -> Unit,
  ) {
	  val canClear = selectedFlagIds.isNotEmpty() || completedStepIds.isNotEmpty()
	  CivicCard {
	    Row(
	      modifier = Modifier.fillMaxWidth(),
	      horizontalArrangement = Arrangement.spacedBy(12.dp),
	      verticalAlignment = Alignment.CenterVertically,
	    ) {
	      Row(
	        modifier =
	          Modifier
	            .weight(1f)
	            .clickable { onToggleExpanded() }
	            .padding(vertical = 6.dp),
	        horizontalArrangement = Arrangement.spacedBy(10.dp),
	        verticalAlignment = Alignment.CenterVertically,
	      ) {
	        Icon(
	          imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
	          contentDescription = if (expanded) "Collapse" else "Expand",
	          tint = MaterialTheme.colorScheme.onSurfaceVariant,
	        )
	        Text(
	          text = stringResource(R.string.shield_red_flags),
	          style = MaterialTheme.typography.titleMedium,
	          fontWeight = FontWeight.Bold,
	        )
	        if (!expanded) {
	          Spacer(modifier = Modifier.weight(1f))
	          Text(
	            text = stringResource(R.string.shield_flags_selected_summary, selectedFlagIds.size),
	            style = MaterialTheme.typography.labelMedium,
	            color = MaterialTheme.colorScheme.onSurfaceVariant,
	          )
	        }
	      }
	      CivicLinkButton(
	        text = stringResource(R.string.shield_clear_all),
	        onClick = onClearAll,
	        enabled = canClear,
	        modifier = Modifier.testTag("shield_clear_all"),
	      )
	    }
	    if (!expanded) return@CivicCard
	    for ((index, flag) in FLAGS.withIndex()) {
	      ShieldFlagTile(
	        title = stringResource(flag.title),
        detail = stringResource(flag.detail),
        checked = selectedFlagIds.contains(flag.id),
        testTag = "shield_flag_$index",
        onChange = { onToggleFlag(flag.id, it) },
      )
    }
  }
}

@Composable
private fun ActionPlanCard(
  completedStepIds: Set<String>,
  onToggleStep: (String, Boolean) -> Unit,
  onCopySummary: () -> Unit,
  onShareSummary: () -> Unit,
  expanded: Boolean,
  onToggleExpanded: () -> Unit,
	) {
	  val done = completedStepIds.size
	  CivicCard {
	    Row(
	      modifier = Modifier.fillMaxWidth(),
	      horizontalArrangement = Arrangement.spacedBy(12.dp),
	      verticalAlignment = Alignment.CenterVertically,
	    ) {
	      Row(
	        modifier =
	          Modifier
	            .weight(1f)
	            .clickable { onToggleExpanded() }
	            .padding(vertical = 6.dp),
	        horizontalArrangement = Arrangement.spacedBy(10.dp),
	        verticalAlignment = Alignment.CenterVertically,
	      ) {
	        Icon(
	          imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
	          contentDescription = if (expanded) "Collapse" else "Expand",
	          tint = MaterialTheme.colorScheme.onSurfaceVariant,
	        )
	        Text(
	          text = stringResource(R.string.shield_action_plan),
	          style = MaterialTheme.typography.titleMedium,
	          fontWeight = FontWeight.Bold,
	        )
	        Spacer(modifier = Modifier.weight(1f))
	        InfoBadge(text = stringResource(R.string.shield_plan_progress, done, STEPS.size))
	      }
	    }

	    if (!expanded) return@CivicCard
	    for ((index, step) in STEPS.withIndex()) {
      PlanStepTile(
        number = index + 1,
        text = stringResource(step.text),
        checked = completedStepIds.contains(step.id),
        testTag = "shield_step_$index",
        onChange = { onToggleStep(step.id, it) },
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      CivicTonalButton(
        text = stringResource(R.string.shield_copy_summary),
        icon = Icons.Default.ContentCopy,
        onClick = onCopySummary,
        modifier =
          Modifier
            .weight(1f)
            .testTag("shield_copy_summary"),
      )
      CivicTonalButton(
        text = stringResource(R.string.shield_share_summary),
        icon = Icons.Default.Share,
        onClick = onShareSummary,
        modifier =
          Modifier
            .weight(1f)
            .testTag("shield_share_summary"),
      )
    }
  }
}

@Composable
private fun EvidenceVaultCard(
  onOpenVault: () -> Unit,
) {
  CivicCard(modifier = Modifier.testTag("shield_vault_card")) {
    Text(text = stringResource(R.string.shield_vault_title), style = MaterialTheme.typography.titleMedium)
    Text(
      text = stringResource(R.string.shield_vault_body),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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
}

@Composable
private fun QuickNextActionsCard(
  onGoVerify: () -> Unit,
  onGoHelp: () -> Unit,
) {
  CivicCard {
    Text(text = stringResource(R.string.shield_next_actions_title), style = MaterialTheme.typography.titleMedium)

    CivicListItem(
      title = stringResource(R.string.shield_next_verify_title),
      subtitle = stringResource(R.string.shield_next_verify_body),
      onClick = onGoVerify,
    )
    CivicListItem(
      title = stringResource(R.string.shield_next_help_title),
      subtitle = stringResource(R.string.shield_next_help_body),
      onClick = onGoHelp,
    )
  }
}

private fun copyToClipboard(ctx: Context, text: String) {
  val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  cm.setPrimaryClip(ClipData.newPlainText("RegShield Shield Summary", text))
}

private fun shareText(ctx: Context, text: String) {
  val intent =
    Intent(Intent.ACTION_SEND)
      .setType("text/plain")
      .putExtra(Intent.EXTRA_TEXT, text)
  ctx.startActivity(Intent.createChooser(intent, "Share"))
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
	          Text(
	            text = detail,
	            style = MaterialTheme.typography.bodySmall,
	            color =
	              if (checked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f)
	              else MaterialTheme.colorScheme.onSurfaceVariant,
	          )
	        }
	      }
	    }
	  }
	}

@Composable
private fun PlanStepTile(
  number: Int,
  text: String,
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
      accent.accent.copy(alpha = 0.45f)
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
      Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
      Checkbox(
        checked = checked,
        onCheckedChange = onChange,
        colors =
          CheckboxDefaults.colors(
            checkedColor = accent.accent,
            checkmarkColor = accent.onAccent,
          ),
      )
    }
  }
}
