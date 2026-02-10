package org.regshield.ca.ui.screens.help

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.regshield.ca.api.AidOrg

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

private val LANGUAGE_OPTIONS = listOf(
  "",
  "English",
  "Spanish",
  "Chinese",
  "Korean",
  "Vietnamese",
)

private val SERVICE_OPTIONS = listOf(
  "",
  "Asylum",
  "Family",
  "VAWA",
  "Work authorization",
  "Naturalization",
  "Removal defense",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
  vm: HelpViewModel = hiltViewModel(),
) {
  val ctx = LocalContext.current
  val st by vm.state.collectAsState()

  Column(modifier = Modifier.fillMaxSize()) {
    TopAppBar(
      title = { Text("Help") },
      scrollBehavior = androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
    )

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Dropdown(
        label = "County",
        options = COUNTY_OPTIONS,
        value = st.filters.county,
        onSelect = { vm.setCounty(it) },
      )
      Dropdown(
        label = "Language",
        options = LANGUAGE_OPTIONS,
        value = st.filters.language,
        onSelect = { vm.setLanguage(it) },
      )
      Dropdown(
        label = "Service",
        options = SERVICE_OPTIONS,
        value = st.filters.service,
        onSelect = { vm.setService(it) },
      )

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { vm.search() }) { Text("Search") }
      }

      if (st.loading) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
          CircularProgressIndicator()
        }
      }
      st.error?.let { Text(it, fontWeight = FontWeight.SemiBold) }
    }

    HorizontalDivider()

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(st.results) { org ->
        OrgCard(
          org = org,
          onOpen = {
            val url = org.website ?: org.sourceUrl ?: return@OrgCard
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
          },
        )
      }
      if (!st.loading && st.results.isEmpty()) {
        item {
          Spacer(Modifier.height(8.dp))
          Text("Search to find organizations. This directory is informational and may change.")
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Dropdown(
  label: String,
  options: List<String>,
  value: String,
  onSelect: (String) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  val display = if (value.isBlank()) "Any" else value

  Box(modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
      value = display,
      onValueChange = {},
      readOnly = true,
      label = { Text(label) },
      modifier = Modifier
        .fillMaxWidth()
        .clickable { expanded = true },
    )
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
    ) {
      options.forEach { opt ->
        val text = if (opt.isBlank()) "Any" else opt
        DropdownMenuItem(
          text = { Text(text) },
          onClick = {
            onSelect(opt)
            expanded = false
          },
        )
      }
    }
  }
}

@Composable
private fun OrgCard(
  org: AidOrg,
  onOpen: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(enabled = !org.website.isNullOrBlank() || !org.sourceUrl.isNullOrBlank()) { onOpen() },
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(org.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(org.intakeStatus)
      }

      val meta = listOfNotNull(
        org.counties.firstOrNull(),
        org.languages.take(2).takeIf { it.isNotEmpty() }?.joinToString(", "),
      ).joinToString(" • ")
      if (meta.isNotBlank()) Text(meta)

      org.phone?.let { Text(it) }
      org.verifiedAt?.let { Text("Last verified: $it") }

      if (!org.sourceUrl.isNullOrBlank()) Text("Source available")
    }
  }
}
