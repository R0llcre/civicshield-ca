package org.regshield.ca.ui.screens.verify

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.regshield.ca.api.RaEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyScreen(
  vm: VerifyViewModel = hiltViewModel(),
) {
  val ctx = LocalContext.current
  val st by vm.state.collectAsState()

  var name by remember { mutableStateOf("") }
  var state by remember { mutableStateOf("CA") }

  Column(modifier = Modifier.fillMaxSize()) {
    TopAppBar(
      title = { Text("Verify") },
      scrollBehavior = androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
    )

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Name") },
        singleLine = true,
      )
      OutlinedTextField(
        value = state,
        onValueChange = { state = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("State (2-letter)") },
        singleLine = true,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { vm.search(name, state) }) { Text("Search") }
      }

      if (st.loading) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
        ) {
          CircularProgressIndicator()
        }
      }

      st.error?.let { msg ->
        Text(msg, fontWeight = FontWeight.SemiBold)
      }

      st.dataFreshnessEndedAt?.let { ended ->
        Text("Roster updated: $ended")
      }
    }

    HorizontalDivider()

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(st.results) { r ->
        ResultCard(
          r = r,
          onOpenSource = {
            val url = r.sourceUrl ?: return@ResultCard
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
          },
        )
      }

      if (!st.loading && st.results.isEmpty()) {
        item {
          Spacer(Modifier.height(8.dp))
          Text("No results yet. Search by name to see matches.")
        }
      }
    }
  }
}

@Composable
private fun ResultCard(
  r: RaEntry,
  onOpenSource: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(enabled = !r.sourceUrl.isNullOrBlank()) { onOpenSource() },
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(r.name, fontWeight = FontWeight.SemiBold)
      val orgLine = listOfNotNull(r.organizationName, r.city, r.state).joinToString(" • ")
      if (orgLine.isNotBlank()) Text(orgLine)
      val meta = buildString {
        if (!r.entryType.isNullOrBlank()) append(r.entryType)
        if (r.score != null) {
          if (isNotEmpty()) append(" • ")
          append("score ${r.score}")
        }
        if (!r.matchedOn.isNullOrBlank()) {
          if (isNotEmpty()) append(" • ")
          append("matched on ${r.matchedOn}")
        }
      }
      if (meta.isNotBlank()) Text(meta)
      if (!r.sourceUrl.isNullOrBlank()) {
        Text("Open official source")
      } else {
        Text("No source link available")
      }
    }
  }
}
