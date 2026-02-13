package org.regshield.ca.ui.screens.verify

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.regshield.ca.R
import org.regshield.ca.api.RaEntry
import org.regshield.ca.ui.components.CivicDropdownField
import org.regshield.ca.ui.components.CivicPrimaryButton
import org.regshield.ca.ui.components.CivicCard
import org.regshield.ca.ui.components.CivicListItem
import org.regshield.ca.ui.components.CivicTextField
import org.regshield.ca.ui.components.HeroPanel
import org.regshield.ca.ui.components.InfoBadge
import org.regshield.ca.ui.components.SplitColumns

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
    loading = st.loading,
    error = st.error,
    dataFreshnessEndedAt = st.dataFreshnessEndedAt,
    results = st.results,
    onNameChange = { name = it },
    onStateChange = { stateCode = it },
    onSearch = { vm.search(name = name, state = stateCode) },
    onOpenSource = { url ->
      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
  loading: Boolean,
  error: String?,
  dataFreshnessEndedAt: String?,
  results: List<RaEntry>,
  onNameChange: (String) -> Unit,
  onStateChange: (String) -> Unit,
  onSearch: () -> Unit,
  onOpenSource: (String) -> Unit,
  onOpenCalBar: () -> Unit,
  onOpenSos: () -> Unit,
) {
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
        title = stringResource(R.string.verify_title),
        subtitle = stringResource(R.string.verify_subtitle),
        badge = stringResource(R.string.nav_verify),
      )
    }

    item {
      SplitColumns(
        isCompact = isCompact,
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

    if (!error.isNullOrBlank()) {
      item {
        CivicCard {
          Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag("verify_error_text"),
          )
        }
      }
    }

    if (loading) {
      item {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(modifier = Modifier.testTag("verify_loading"))
        }
      }
    }

    if (!loading && results.isEmpty() && name.isNotBlank()) {
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
        onOpenSource = onOpenSource,
        modifier = Modifier.testTag("verify_result_item_$index"),
      )
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
private fun VerifyResultCard(
  result: RaEntry,
  onOpenSource: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  CivicCard(
    modifier =
      modifier.then(
        if (result.sourceUrl.isNullOrBlank()) {
          Modifier
        } else {
          Modifier.clickable { onOpenSource(result.sourceUrl.orEmpty()) }
        },
      ),
  ) {
    Text(text = result.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    val orgLine = listOfNotNull(result.organizationName, result.city, result.state).joinToString(" • ")
    if (orgLine.isNotBlank()) {
      Text(text = orgLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (result.score != null) {
      InfoBadge(text = stringResource(R.string.verify_score, result.score.toString()))
    }
    if (!result.matchedOn.isNullOrBlank()) {
      Text(
        text = stringResource(R.string.verify_matched_on, result.matchedOn),
        style = MaterialTheme.typography.bodySmall,
      )
    }

    result.entryType?.let {
      Text(text = it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
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
    result.phone?.let {
      Text(text = it, style = MaterialTheme.typography.bodySmall)
    }
    if (!result.sourceUrl.isNullOrBlank()) {
      Text(text = stringResource(R.string.common_open_source), style = MaterialTheme.typography.labelLarge)
    } else {
      Text(text = stringResource(R.string.common_no_source), style = MaterialTheme.typography.bodySmall)
    }
  }
}
