package org.regshield.ca.ui.screens.help

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.regshield.ca.R
import org.regshield.ca.api.AidOrg
import org.regshield.ca.ui.components.CivicDropdownField
import org.regshield.ca.ui.components.CivicLinkButton
import org.regshield.ca.ui.components.CivicPrimaryButton
import org.regshield.ca.ui.components.CivicCard
import org.regshield.ca.ui.components.HeroPanel
import org.regshield.ca.ui.components.SplitColumns
import org.regshield.ca.ui.components.StatusChip

private val COUNTY_OPTIONS = listOf(
  "",
  "Los Angeles",
  "San Diego",
  "Orange",
  "San Francisco",
  "Santa Clara",
  "Alameda",
  "Sacramento",
  "Riverside",
  "San Bernardino",
  "Fresno",
  "Ventura",
)

private val LANGUAGE_OPTIONS = listOf("", "English", "Spanish", "Chinese", "Korean", "Vietnamese", "Tagalog")
private val SERVICE_OPTIONS = listOf("", "Asylum", "Family", "VAWA", "Work authorization", "Naturalization", "Removal defense")

@Composable
fun HelpScreen(
  isCompact: Boolean,
  contentPadding: PaddingValues = PaddingValues(0.dp),
  vm: HelpViewModel = hiltViewModel(),
) {
  val ctx = LocalContext.current
  val st by vm.state.collectAsState()

  fun openUrl(url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
  }

  HelpContent(
    isCompact = isCompact,
    contentPadding = contentPadding,
    state = st,
    onCounty = vm::setCounty,
    onLanguage = vm::setLanguage,
    onService = vm::setService,
    onSearch = vm::search,
    onOpenOrgLink = { org ->
      val url = org.website ?: org.sourceUrl ?: return@HelpContent
      openUrl(url)
    },
    onOpenUrl = ::openUrl,
  )
}

@Composable
fun HelpContent(
  isCompact: Boolean,
  contentPadding: PaddingValues = PaddingValues(0.dp),
  state: HelpUiState,
  onCounty: (String) -> Unit,
  onLanguage: (String) -> Unit,
  onService: (String) -> Unit,
  onSearch: () -> Unit,
  onOpenOrgLink: (AidOrg) -> Unit,
  onOpenUrl: (String) -> Unit,
) {
  val chromeTopPadding = contentPadding.calculateTopPadding()
  val chromeBottomPadding = contentPadding.calculateBottomPadding()

  LazyColumn(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(top = chromeTopPadding)
        .imePadding()
        .testTag("help_scroll"),
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
        title = stringResource(R.string.help_title),
        subtitle = stringResource(R.string.help_subtitle),
        badge = stringResource(R.string.nav_help),
      )
    }

    item {
      SplitColumns(
        isCompact = isCompact,
        modifier = Modifier.fillMaxWidth(),
        left = {
          HelpFiltersCard(
            state = state,
            onCounty = onCounty,
            onLanguage = onLanguage,
            onService = onService,
            onSearch = onSearch,
          )
        },
        right = {
          CivicCard {
            Text(text = stringResource(R.string.help_official_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
              text = stringResource(R.string.help_official_hint),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CivicLinkButton(
              text = stringResource(R.string.help_official_lawhelpca),
              icon = Icons.AutoMirrored.Filled.OpenInNew,
              onClick = { onOpenUrl("https://lawhelpca.org/") },
            )
            CivicLinkButton(
              text = stringResource(R.string.help_official_imm_adv),
              icon = Icons.AutoMirrored.Filled.OpenInNew,
              onClick = { onOpenUrl("https://www.immigrationadvocates.org/") },
            )
            CivicLinkButton(
              text = stringResource(R.string.help_official_eoir_ra),
              icon = Icons.AutoMirrored.Filled.OpenInNew,
              onClick = { onOpenUrl("https://www.justice.gov/eoir/recognition-and-accreditation-roster-reports") },
            )
            Text(
              text = stringResource(R.string.verify_disclaimer),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
      )
    }

    if (state.loading) {
      item {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(modifier = Modifier.testTag("help_loading"))
        }
      }
    }

    if (!state.error.isNullOrBlank()) {
      item {
        CivicCard {
          Text(
            text = state.error.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag("help_error_text"),
          )
        }
      }
    }

    if (state.hasSearched && !state.loading && state.results.isEmpty()) {
      item {
        CivicCard {
          Text(text = stringResource(R.string.help_no_results), style = MaterialTheme.typography.bodyMedium)
        }
      }
    }

    itemsIndexed(state.results) { index, org ->
      HelpOrgCard(
        org = org,
        modifier = Modifier.testTag("help_result_item_$index"),
        onOpen = { onOpenOrgLink(org) },
      )
    }
  }
}

@Composable
private fun HelpFiltersCard(
  state: HelpUiState,
  onCounty: (String) -> Unit,
  onLanguage: (String) -> Unit,
  onService: (String) -> Unit,
  onSearch: () -> Unit,
) {
  CivicCard {
    val anyLabel = stringResource(R.string.common_any)
    CivicDropdownField(
      label = stringResource(R.string.help_county_label),
      options = COUNTY_OPTIONS,
      value = state.filters.county,
      onSelect = onCounty,
      placeholder = anyLabel,
      testTag = "help_county_filter",
    )
    CivicDropdownField(
      label = stringResource(R.string.help_language_label),
      options = LANGUAGE_OPTIONS,
      value = state.filters.language,
      onSelect = onLanguage,
      placeholder = anyLabel,
      testTag = "help_language_filter",
    )
    CivicDropdownField(
      label = stringResource(R.string.help_service_label),
      options = SERVICE_OPTIONS,
      value = state.filters.service,
      onSelect = onService,
      placeholder = anyLabel,
      testTag = "help_service_filter",
    )

    CivicPrimaryButton(
      text = stringResource(R.string.help_search_button),
      icon = Icons.Default.Search,
      onClick = onSearch,
      modifier =
        Modifier
          .fillMaxWidth()
          .testTag("help_search_button"),
    )
  }
}

@Composable
private fun HelpOrgCard(
  org: AidOrg,
  modifier: Modifier = Modifier,
  onOpen: () -> Unit,
) {
  CivicCard(
    modifier = modifier.then(Modifier.clickable(enabled = !org.website.isNullOrBlank() || !org.sourceUrl.isNullOrBlank()) { onOpen() }),
  ) {
    Text(text = org.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    StatusChip(
      status = when (org.intakeStatus.lowercase()) {
        "open" -> stringResource(R.string.common_status_open)
        "waitlist" -> stringResource(R.string.common_status_waitlist)
        "closed" -> stringResource(R.string.common_status_closed)
        else -> org.intakeStatus
      },
      modifier = Modifier.testTag("help_status_chip"),
    )

    val counties = org.counties.joinToString(", ")
    if (counties.isNotBlank()) Text(text = stringResource(R.string.help_counties, counties), style = MaterialTheme.typography.bodySmall)
    val languages = org.languages.joinToString(", ")
    if (languages.isNotBlank()) Text(text = stringResource(R.string.help_languages, languages), style = MaterialTheme.typography.bodySmall)
    val services = org.services.joinToString(", ")
    if (services.isNotBlank()) Text(text = stringResource(R.string.help_services, services), style = MaterialTheme.typography.bodySmall)
    org.etaCallbackDays?.let { Text(text = stringResource(R.string.help_eta, it), style = MaterialTheme.typography.bodySmall) }
    org.nextClinicAt?.let { Text(text = stringResource(R.string.help_next_clinic, it), style = MaterialTheme.typography.bodySmall) }
    org.verifiedAt?.let { Text(text = stringResource(R.string.common_last_verified, it), style = MaterialTheme.typography.bodySmall) }

    HorizontalDivider()
    if (!org.sourceUrl.isNullOrBlank() || !org.website.isNullOrBlank()) {
      CivicLinkButton(
        text = stringResource(R.string.common_open_source),
        icon = Icons.AutoMirrored.Filled.OpenInNew,
        onClick = onOpen,
        modifier = Modifier.align(Alignment.End),
      )
    } else {
      Text(
        text = stringResource(R.string.common_no_source),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
