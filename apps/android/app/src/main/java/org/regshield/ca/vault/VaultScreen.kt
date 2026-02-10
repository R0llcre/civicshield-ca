package org.regshield.ca.vault

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
  onClose: () -> Unit,
  vm: VaultViewModel = hiltViewModel(),
) {
  val ctx = LocalContext.current
  val scope = rememberCoroutineScope()
  val st by vm.state.collectAsState()

  var passphrase by remember { mutableStateOf("") }

  val addFilesLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments(),
    onResult = { uris -> vm.addFiles(uris) },
  )

  val importLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
    onResult = { uri ->
      if (uri == null) return@rememberLauncherForActivityResult
      scope.launch {
        val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
        vm.importZip(passphrase, bytes)
      }
    },
  )

  val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/zip"),
    onResult = { uri ->
      if (uri == null) return@rememberLauncherForActivityResult
      scope.launch {
        runCatching {
          val bytes = vm.exportZipBytes()
          ctx.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        }
      }
    },
  )

  Column(modifier = Modifier.fillMaxSize()) {
    TopAppBar(
      title = { Text("Evidence Vault") },
      actions = {
        TextButton(onClick = onClose) { Text("Close") }
      },
      scrollBehavior = androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
    )

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("Local-only encrypted storage. Files never upload by default.", fontWeight = FontWeight.SemiBold)

      OutlinedTextField(
        value = passphrase,
        onValueChange = { passphrase = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Passphrase") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
      )

      if (st.busy) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
          CircularProgressIndicator()
        }
      }

      st.error?.let { Text(it, fontWeight = FontWeight.SemiBold) }

      if (st.locked) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          if (st.hasVault) {
            Button(onClick = { vm.unlock(passphrase) }, enabled = !st.busy) { Text("Unlock") }
          } else {
            Button(onClick = { vm.createNew(passphrase) }, enabled = !st.busy) { Text("Create New") }
          }
          Button(onClick = { importLauncher.launch(arrayOf("application/zip")) }, enabled = !st.busy) {
            Text("Import Zip")
          }
        }
        Text("Tip: keep your passphrase safe. If you forget it, we cannot recover your vault.")
      } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Button(onClick = { addFilesLauncher.launch(arrayOf("*/*")) }, enabled = !st.busy) { Text("Add Files") }
          Button(onClick = { exportLauncher.launch("regshield_vault.zip") }, enabled = !st.busy) { Text("Export Zip") }
          Button(onClick = { vm.lock() }, enabled = !st.busy) { Text("Lock") }
        }
      }
    }

    HorizontalDivider()

    if (!st.locked) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        items(st.items) { it ->
          Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(it.originalName, fontWeight = FontWeight.SemiBold)
              Text("${it.contentType} • ${it.size} bytes")
              Text(it.createdAt)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                TextButton(onClick = { vm.deleteItem(it.id) }, enabled = !st.busy) { Text("Delete") }
              }
            }
          }
        }
        if (st.items.isEmpty()) {
          item {
            Spacer(Modifier.height(8.dp))
            Text("No files yet. Add files to build an evidence package.")
          }
        }
      }
    }
  }
}
