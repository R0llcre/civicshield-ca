package org.regshield.ca.vault

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import org.regshield.ca.R
import org.regshield.ca.ui.components.CivicLinkButton
import org.regshield.ca.ui.components.CivicPrimaryButton
import org.regshield.ca.ui.components.CivicTextField
import org.regshield.ca.ui.components.CivicTonalButton
import org.regshield.ca.ui.components.CivicCard
import org.regshield.ca.ui.components.HeroPanel
import java.text.DecimalFormat

@Composable
fun VaultScreen(
  isCompact: Boolean,
  contentPadding: PaddingValues = PaddingValues(0.dp),
  onClose: () -> Unit,
  vm: VaultViewModel = hiltViewModel(),
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val state by vm.state.collectAsState()
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
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
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
          context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        }
      }
    },
  )

  VaultContent(
    isCompact = isCompact,
    contentPadding = contentPadding,
    state = state,
    passphrase = passphrase,
    onPassphraseChange = { passphrase = it },
    onClose = onClose,
    onUnlock = { vm.unlock(passphrase) },
    onCreate = { vm.createNew(passphrase) },
    onImport = { importLauncher.launch(arrayOf("application/zip")) },
    onAddFiles = { addFilesLauncher.launch(arrayOf("*/*")) },
    onExport = { exportLauncher.launch("regshield_vault.zip") },
    onLock = vm::lock,
    onDelete = vm::deleteItem,
  )
}

@Composable
fun VaultContent(
  isCompact: Boolean,
  contentPadding: PaddingValues = PaddingValues(0.dp),
  state: VaultUiState,
  passphrase: String,
  onPassphraseChange: (String) -> Unit,
  onClose: () -> Unit,
  onUnlock: () -> Unit,
  onCreate: () -> Unit,
  onImport: () -> Unit,
  onAddFiles: () -> Unit,
  onExport: () -> Unit,
  onLock: () -> Unit,
  onDelete: (String) -> Unit,
) {
  val chromeTopPadding = contentPadding.calculateTopPadding()
  val chromeBottomPadding = contentPadding.calculateBottomPadding()

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
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
      ) {
        TextButton(onClick = onClose) { Text(stringResource(R.string.common_close)) }
      }
    }

    item {
      HeroPanel(
        title = stringResource(R.string.vault_title),
        subtitle = stringResource(R.string.vault_subtitle),
        badge =
          if (state.locked) stringResource(R.string.vault_badge_locked)
          else stringResource(R.string.vault_badge_unlocked),
        icon = if (state.locked) Icons.Default.Lock else Icons.Default.LockOpen,
      )
    }

    item {
      CivicCard {
        CivicTextField(
          value = passphrase,
          onValueChange = onPassphraseChange,
          label = stringResource(R.string.vault_passphrase_label),
          visualTransformation = PasswordVisualTransformation(),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          testTag = "vault_passphrase_input",
        )
        Text(
          text = stringResource(R.string.vault_tip),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.busy) {
          Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.testTag("vault_loading"))
          }
        }

        if (!state.error.isNullOrBlank()) {
          Text(text = state.error.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("vault_error_text"))
        }

        if (state.locked) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.hasVault) {
              CivicPrimaryButton(
                text = stringResource(R.string.vault_unlock),
                icon = Icons.Default.LockOpen,
                onClick = onUnlock,
                enabled = !state.busy,
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .testTag("vault_unlock_button"),
              )
            } else {
              CivicPrimaryButton(
                text = stringResource(R.string.vault_create_new),
                icon = Icons.Default.Lock,
                onClick = onCreate,
                enabled = !state.busy,
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .testTag("vault_create_button"),
              )
            }
            CivicTonalButton(
              text = stringResource(R.string.vault_import_zip),
              icon = Icons.Default.UploadFile,
              onClick = onImport,
              enabled = !state.busy,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CivicPrimaryButton(
              text = stringResource(R.string.vault_add_files),
              icon = Icons.Default.FileOpen,
              onClick = onAddFiles,
              enabled = !state.busy,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .testTag("vault_add_files_button"),
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              CivicTonalButton(
                text = stringResource(R.string.vault_export_zip),
                icon = Icons.Default.FileDownload,
                onClick = onExport,
                enabled = !state.busy,
                modifier = Modifier.weight(1f),
              )
              CivicTonalButton(
                text = stringResource(R.string.vault_lock),
                icon = Icons.Default.Lock,
                onClick = onLock,
                enabled = !state.busy,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }
    }

    if (!state.locked) {
      if (state.items.isEmpty()) {
        item {
          CivicCard {
            Text(text = stringResource(R.string.vault_no_files), style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
      itemsIndexed(state.items) { index, item ->
        CivicCard(modifier = Modifier.testTag("vault_item_$index")) {
          Text(text = item.originalName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text(text = item.contentType, style = MaterialTheme.typography.bodySmall)
          Text(text = "${humanReadableBytes(item.size)} · ${item.createdAt}", style = MaterialTheme.typography.bodySmall)
          TextButton(onClick = { onDelete(item.id) }, enabled = !state.busy, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.vault_delete))
          }
        }
      }
    }
  }
}

private fun humanReadableBytes(bytes: Long): String {
  if (bytes < 1024) return "$bytes B"
  val exp = (kotlin.math.ln(bytes.toDouble()) / kotlin.math.ln(1024.0)).toInt()
  val pre = "KMGTPE"[exp - 1]
  val value = bytes / Math.pow(1024.0, exp.toDouble())
  return "${DecimalFormat("#,##0.#").format(value)} ${pre}B"
}
