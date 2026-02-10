package org.regshield.ca.ui.screens.shield

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class Flag(val id: String, val title: String, val detail: String)

private val FLAGS = listOf(
  Flag("pay", "Asks for payment by gift card or crypto", "Legitimate legal services rarely require these payment methods."),
  Flag("rush", "Pressures you to act immediately", "Scams often create urgency to stop you from verifying details."),
  Flag("no_bar", "Refuses to share license/registration details", "A legitimate provider should be verifiable via official directories."),
  Flag("docs", "Asks you to send originals or sensitive documents", "Avoid sending originals; keep copies and verify before sharing."),
  Flag("offsite", "Only communicates off-platform", "Prefer official channels and keep written records."),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShieldScreen(
  onOpenVault: () -> Unit,
) {
  val checked = remember { mutableStateMapOf<String, Boolean>() }

  Column(modifier = Modifier.fillMaxSize()) {
    TopAppBar(
      title = { Text("Shield") },
      scrollBehavior = androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
    )

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("Red flags checklist", fontWeight = FontWeight.SemiBold)
      Text("This is informational and not legal advice.")

      Button(onClick = onOpenVault) { Text("Evidence Vault (local only)") }
    }

    HorizontalDivider()

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(FLAGS) { f ->
        val isChecked = checked[f.id] == true
        Card(modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Checkbox(
                checked = isChecked,
                onCheckedChange = { checked[f.id] = it },
              )
              Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(f.title, fontWeight = FontWeight.SemiBold)
                Text(f.detail)
              }
            }
          }
        }
      }

      item {
        Spacer(Modifier.height(8.dp))
        Text("If something feels wrong, stop and verify the person or organization via official sources before paying or sharing documents.")
      }
    }
  }
}
