package org.regshield.ca.ui.screens.verify

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.regshield.ca.R
import org.regshield.ca.api.RaEntry
import org.regshield.ca.ui.components.CivicDropdownField
import org.regshield.ca.ui.components.CivicLinkButton
import org.regshield.ca.ui.components.CivicPrimaryButton
import org.regshield.ca.ui.components.CivicCard
import org.regshield.ca.ui.components.CivicListItem
import org.regshield.ca.ui.components.CivicTextField
import org.regshield.ca.ui.components.HeroPanel
import org.regshield.ca.ui.components.InfoBadge
import org.regshield.ca.ui.components.SplitColumns
import org.regshield.ca.ui.theme.LocalCivicAccent

private data class StateOption(val code: String, val name: String)

private val STATE_OPTIONS = listOf(
  StateOption("CA", "California"),
  StateOption("AL", "Alabama"),
  StateOption("AK", "Alaska"),
  StateOption("AZ", "Arizona"),
  StateOption("AR", "Arkansas"),
  StateOption("CO", "Colorado"),
  StateOption("CT", "Connecticut"),
  StateOption("DE", "Delaware"),
  StateOption("DC", "District of Columbia"),
  StateOption("FL", "Florida"),
  StateOption("GA", "Georgia"),
  StateOption("HI", "Hawaii"),
  StateOption("ID", "Idaho"),
  StateOption("IL", "Illinois"),
  StateOption("IN", "Indiana"),
  StateOption("IA", "Iowa"),
  StateOption("KS", "Kansas"),
  StateOption("KY", "Kentucky"),
  StateOption("LA", "Louisiana"),
  StateOption("ME", "Maine"),
  StateOption("MD", "Maryland"),
  StateOption("MA", "Massachusetts"),
  StateOption("MI", "Michigan"),
  StateOption("MN", "Minnesota"),
  StateOption("MS", "Mississippi"),
  StateOption("MO", "Missouri"),
  StateOption("MT", "Montana"),
  StateOption("NE", "Nebraska"),
  StateOption("NV", "Nevada"),
  StateOption("NH", "New Hampshire"),
  StateOption("NJ", "New Jersey"),
  StateOption("NM", "New Mexico"),
  StateOption("NY", "New York"),
  StateOption("NC", "North Carolina"),
  StateOption("ND", "North Dakota"),
  StateOption("OH", "Ohio"),
  StateOption("OK", "Oklahoma"),
  StateOption("OR", "Oregon"),
  StateOption("PA", "Pennsylvania"),
  StateOption("RI", "Rhode Island"),
  StateOption("SC", "South Carolina"),
  StateOption("SD", "South Dakota"),
  StateOption("TN", "Tennessee"),
  StateOption("TX", "Texas"),
  StateOption("UT", "Utah"),
  StateOption("VT", "Vermont"),
  StateOption("VA", "Virginia"),
  StateOption("WA", "Washington"),
  StateOption("WV", "West Virginia"),
  StateOption("WI", "Wisconsin"),
  StateOption("WY", "Wyoming"),
)

@Composable
fun VerifyScreen(
  isCompact: Boolean,
  contentPadding: PaddingValues = PaddingValues(0.dp),
  vm: VerifyViewModel = hiltViewModel(),
) {
  val context = LocalContext.current
  val st by vm.state.collectAsState()
  var name by remember { mutableStateOf("") }
  var stateCode by remember { mutableStateOf("CA") }

  VerifyContent(
    isCompact = isCompact,
    contentPadding = contentPadding,
    name = name,
    stateCode = stateCode,
    hasSearched = st.hasSearched,
    queryName = st.queryName,
    queryState = st.queryState,
    loading = st.loading,
    errorResId = st.errorResId,
    errorMessage = st.errorMessage,
    dataFreshnessEndedAt = st.dataFreshnessEndedAt,
    results = st.results,
    onNameChange = {
      name = it
      vm.clearError()
    },
    onStateChange = {
      stateCode = it
      vm.clearError()
    },
    onSearch = { vm.search(name = name, state = stateCode) },
    onOpenUrl = { url ->
      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    },
    onCallPhone = { phone ->
      context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
    },
    onOpenCalBar = {
      context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse("https://apps.calbar.ca.gov/attorney/LicenseeSearch/QuickSearch")),
      )
    },
    onOpenSos = {
      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://specialfilings.sos.ca.gov/icbs")))
    },
  )
}

@Composable
fun VerifyContent(
  isCompact: Boolean,
  contentPadding: PaddingValues = PaddingValues(0.dp),
  name: String,
  stateCode: String,
  hasSearched: Boolean,
  queryName: String?,
  queryState: String?,
  loading: Boolean,
  errorResId: Int?,
  errorMessage: String?,
  dataFreshnessEndedAt: String?,
  results: List<RaEntry>,
  onNameChange: (String) -> Unit,
  onStateChange: (String) -> Unit,
  onSearch: () -> Unit,
  onOpenUrl: (String) -> Unit,
  onCallPhone: (String) -> Unit,
  onOpenCalBar: () -> Unit,
  onOpenSos: () -> Unit,
) {
  val chromeTopPadding = contentPadding.calculateTopPadding()
  val chromeBottomPadding = contentPadding.calculateBottomPadding()
  val errorText =
    when {
      errorResId != null -> stringResource(errorResId)
      !errorMessage.isNullOrBlank() -> errorMessage
      else -> null
    }
  val showResultsSection = hasSearched || loading || results.isNotEmpty()

  LazyColumn(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(top = chromeTopPadding)
        .imePadding(),
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
        title = stringResource(R.string.verify_title),
        subtitle = stringResource(R.string.verify_subtitle),
        badge = stringResource(R.string.nav_verify),
      )
    }

    item {
      if (isCompact) {
        VerifySearchCard(
          name = name,
          stateCode = stateCode,
          loading = loading,
          dataFreshnessEndedAt = dataFreshnessEndedAt,
          onNameChange = onNameChange,
          onStateChange = onStateChange,
          onSearch = onSearch,
        )
      } else {
        SplitColumns(
          isCompact = false,
          modifier = Modifier.fillMaxWidth(),
          left = {
            VerifySearchCard(
              name = name,
              stateCode = stateCode,
              loading = loading,
              dataFreshnessEndedAt = dataFreshnessEndedAt,
              onNameChange = onNameChange,
              onStateChange = onStateChange,
              onSearch = onSearch,
            )
          },
          right = {
            OfficialLinksCard(
              onOpenCalBar = onOpenCalBar,
              onOpenSos = onOpenSos,
            )
          },
        )
      }
    }

    if (!errorText.isNullOrBlank()) {
      item {
        CivicCard {
          Text(
            text = errorText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag("verify_error_text"),
          )
        }
      }
    }

    if (showResultsSection) {
      item {
        ResultsHeaderCard(
          queryName = queryName,
          queryState = queryState,
          count = results.size,
          loading = loading,
        )
      }

      if (!loading && hasSearched && results.isEmpty()) {
        item {
          CivicCard {
            Text(
              text = stringResource(R.string.verify_no_results),
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
      }

      itemsIndexed(results) { index, result ->
        VerifyResultCard(
          result = result,
          onOpenUrl = onOpenUrl,
          onCallPhone = onCallPhone,
          modifier = Modifier.testTag("verify_result_item_$index"),
          matchChipTestTag = "verify_match_chip_$index",
        )
      }
    }

    if (isCompact) {
      item {
        OfficialLinksCollapsibleCard(
          onOpenCalBar = onOpenCalBar,
          onOpenSos = onOpenSos,
        )
      }
    }

    item {
      CivicCard {
        Text(
          text = stringResource(R.string.verify_disclaimer),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun VerifySearchCard(
  name: String,
  stateCode: String,
  loading: Boolean,
  dataFreshnessEndedAt: String?,
  onNameChange: (String) -> Unit,
  onStateChange: (String) -> Unit,
  onSearch: () -> Unit,
) {
  val stateCodes = remember { STATE_OPTIONS.map { it.code } }
  val displayState: (String) -> String = { code ->
    val opt = STATE_OPTIONS.firstOrNull { it.code == code } ?: StateOption("CA", "California")
    "${opt.name} (${opt.code})"
  }

  CivicCard {
    CivicTextField(
      value = name,
      onValueChange = onNameChange,
      label = stringResource(R.string.verify_name_label),
      placeholder = stringResource(R.string.verify_name_placeholder),
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
      testTag = "verify_name_input",
    )

    CivicDropdownField(
      label = stringResource(R.string.verify_state_label),
      options = stateCodes,
      value = stateCode,
      onSelect = onStateChange,
      display = displayState,
      modifier = Modifier.fillMaxWidth(),
      testTag = "verify_state_selector",
    )

    CivicPrimaryButton(
      text = stringResource(R.string.verify_search_button),
      icon = Icons.Default.Search,
      onClick = onSearch,
      enabled = !loading && name.isNotBlank(),
      modifier =
        Modifier
          .fillMaxWidth()
          .testTag("verify_search_button"),
    )

    if (loading) {
      InfoBadge(text = stringResource(R.string.verify_searching))
    }
    if (!dataFreshnessEndedAt.isNullOrBlank()) {
      InfoBadge(text = stringResource(R.string.verify_roster_updated, dataFreshnessEndedAt))
    }
  }
}

@Composable
private fun OfficialLinksCard(
  onOpenCalBar: () -> Unit,
  onOpenSos: () -> Unit,
) {
  CivicCard {
    Text(text = stringResource(R.string.verify_official_links), style = MaterialTheme.typography.titleMedium)
    Text(
      text = stringResource(R.string.verify_official_links_hint),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    HorizontalDivider()

    CivicListItem(
      title = stringResource(R.string.verify_calbar_title),
      subtitle = stringResource(R.string.verify_calbar_body),
      onClick = onOpenCalBar,
      trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
    )

    CivicListItem(
      title = stringResource(R.string.verify_sos_title),
      subtitle = stringResource(R.string.verify_sos_body),
      onClick = onOpenSos,
      trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
    )
  }
}

@Composable
private fun OfficialLinksCollapsibleCard(
  onOpenCalBar: () -> Unit,
  onOpenSos: () -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  CivicCard {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .clickable { expanded = !expanded }
          .padding(bottom = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = stringResource(R.string.verify_official_links), style = MaterialTheme.typography.titleMedium)
        Text(
          text = stringResource(R.string.verify_official_links_hint),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Icon(
        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    AnimatedVisibility(visible = expanded) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        CivicListItem(
          title = stringResource(R.string.verify_calbar_title),
          subtitle = stringResource(R.string.verify_calbar_body),
          onClick = onOpenCalBar,
          trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
        )
        CivicListItem(
          title = stringResource(R.string.verify_sos_title),
          subtitle = stringResource(R.string.verify_sos_body),
          onClick = onOpenSos,
          trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
        )
      }
    }
  }
}

@Composable
private fun ResultsHeaderCard(
  queryName: String?,
  queryState: String?,
  count: Int,
  loading: Boolean,
) {
  CivicCard {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text =
            if (!queryName.isNullOrBlank() && !queryState.isNullOrBlank()) {
              stringResource(R.string.verify_results_for, queryName, queryState)
            } else {
              stringResource(R.string.verify_results_title)
            },
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = stringResource(R.string.verify_results_hint),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (loading) {
        CircularProgressIndicator(
          strokeWidth = 2.dp,
          modifier = Modifier.size(18.dp).testTag("verify_loading"),
        )
      } else {
        InfoBadge(text = count.toString())
      }
    }
  }
}

@Composable
private fun VerifyResultCard(
  result: RaEntry,
  onOpenUrl: (String) -> Unit,
  onCallPhone: (String) -> Unit,
  modifier: Modifier = Modifier,
  matchChipTestTag: String = "",
) {
  val scorePercent = result.score?.let { scoreToPercent(it) }
  val tier = scorePercent?.let { percentToTier(it) }

  CivicCard(
    modifier =
      modifier.then(
        if (result.sourceUrl.isNullOrBlank()) {
          Modifier
        } else {
          Modifier.clickable { onOpenUrl(result.sourceUrl.orEmpty()) }
        },
      ),
    contentPadding = PaddingValues(12.dp),
  ) {
    Text(text = result.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      EntryTypeChip(entryType = result.entryType)
      if (scorePercent != null && tier != null) {
        MatchChip(
          tier = tier,
          percent = scorePercent,
          modifier = Modifier.then(if (matchChipTestTag.isNotBlank()) Modifier.testTag(matchChipTestTag) else Modifier),
        )
      }
    }

    val showUsForRepresentative =
      !(
        result.entryType?.trim()?.lowercase() == "representative" &&
          result.state?.trim()?.uppercase() == "US" &&
          result.city.isNullOrBlank()
      )
    val orgLine =
      buildList<String> {
        if (!result.organizationName.isNullOrBlank()) add(result.organizationName)
        if (!result.city.isNullOrBlank()) add(result.city)
        val st = result.state
        if (!st.isNullOrBlank() && (showUsForRepresentative || st.trim().uppercase() != "US")) add(st)
      }.joinToString(" • ")
    if (orgLine.isNotBlank()) {
      Text(text = orgLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    val matchedOnRes =
      when (result.matchedOn?.trim()?.lowercase()) {
        "name" -> R.string.verify_matched_on_name
        "organization_name" -> R.string.verify_matched_on_org
        null, "" -> null
        else -> R.string.verify_matched_on_other
      }
    if (matchedOnRes != null) {
      Text(
        text = stringResource(matchedOnRes),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    result.accreditationLevel?.let {
      Text(text = it, style = MaterialTheme.typography.bodySmall)
    }
    result.status?.let {
      Text(text = it, style = MaterialTheme.typography.bodySmall)
    }
    result.address?.let {
      Text(text = it, style = MaterialTheme.typography.bodySmall)
    }

    if (!result.sourceUrl.isNullOrBlank()) {
      CivicLinkButton(
        text = stringResource(R.string.common_open_source),
        icon = Icons.AutoMirrored.Filled.OpenInNew,
        onClick = { onOpenUrl(result.sourceUrl.orEmpty()) },
        modifier = Modifier.align(Alignment.End),
      )
    } else {
      Text(
        text = stringResource(R.string.common_no_source),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    if (!result.phone.isNullOrBlank() || !result.website.isNullOrBlank()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (!result.phone.isNullOrBlank()) {
          CivicLinkButton(
            text = stringResource(R.string.verify_action_call),
            icon = Icons.Default.Call,
            onClick = { onCallPhone(result.phone.orEmpty()) },
          )
        }
        val website = result.website
        if (!website.isNullOrBlank()) {
          CivicLinkButton(
            text = stringResource(R.string.verify_action_website),
            icon = Icons.Default.Language,
            onClick = { onOpenUrl(website) },
          )
        }
      }
    }
  }
}

@Composable
private fun EntryTypeChip(entryType: String?) {
  val text =
    when (entryType?.trim()?.lowercase()) {
      "representative" -> stringResource(R.string.verify_entry_representative)
      "organization" -> stringResource(R.string.verify_entry_organization)
      null, "" -> null
      else -> entryType
    } ?: return

  Surface(
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
    )
  }
}

@Composable
private fun MatchChip(
  tier: MatchTier,
  percent: Int,
  modifier: Modifier = Modifier,
) {
  val accent = LocalCivicAccent.current
  val (bg, fg) =
    when (tier) {
      MatchTier.STRONG -> accent.accent to accent.onAccent
      MatchTier.LIKELY -> accent.accentContainer to accent.onAccentContainer
      MatchTier.POSSIBLE -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
      MatchTier.LOW -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
  val labelRes =
    when (tier) {
      MatchTier.STRONG -> R.string.verify_match_strong
      MatchTier.LIKELY -> R.string.verify_match_likely
      MatchTier.POSSIBLE -> R.string.verify_match_possible
      MatchTier.LOW -> R.string.verify_match_low
    }

  Surface(
    modifier = modifier.clip(MaterialTheme.shapes.extraLarge),
    shape = MaterialTheme.shapes.extraLarge,
    color = bg.copy(alpha = if (tier == MatchTier.STRONG) 0.92f else 0.86f),
    contentColor = fg,
  ) {
    Text(
      text = stringResource(R.string.verify_match_chip, stringResource(labelRes), percent),
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
    )
  }
}
