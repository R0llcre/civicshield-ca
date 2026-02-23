package org.regshield.ca.ui.screens.ask

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.DateFormat
import java.util.Locale
import org.regshield.ca.R
import org.regshield.ca.api.AskSuggestedAction
import org.regshield.ca.data.AskThreadMeta
import org.regshield.ca.data.AskStoredMessage
import org.regshield.ca.ui.components.CivicCard
import org.regshield.ca.ui.components.CivicLinkButton
import org.regshield.ca.ui.components.CivicPrimaryButton
import org.regshield.ca.ui.components.CivicTextField
import org.regshield.ca.ui.components.HeroPanel
import org.regshield.ca.ui.components.MarkdownText
import org.regshield.ca.ui.theme.LocalCivicAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskScreen(
  isCompact: Boolean,
  contentPadding: PaddingValues = PaddingValues(0.dp),
  onGoVerify: () -> Unit,
  onGoHelp: () -> Unit,
  vm: AskViewModel = hiltViewModel(),
) {
  val st by vm.state.collectAsState()
  var draft by remember { mutableStateOf("") }
  var showConsent by remember { mutableStateOf(false) }
  var showChats by remember { mutableStateOf(false) }
  var showRename by remember { mutableStateOf(false) }
  var renameTarget by remember { mutableStateOf<AskThreadMeta?>(null) }
  var renameDraft by remember { mutableStateOf("") }
  var showDeleteConfirm by remember { mutableStateOf(false) }
  var deleteTarget by remember { mutableStateOf<AskThreadMeta?>(null) }
  val context = LocalContext.current
  val focusManager = LocalFocusManager.current
  val keyboard = LocalSoftwareKeyboardController.current

  fun openUrl(url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
  }

  fun shareText(text: String) {
    runCatching {
      val intent =
        Intent(Intent.ACTION_SEND)
          .setType("text/plain")
          .putExtra(Intent.EXTRA_TEXT, text)
      context.startActivity(Intent.createChooser(intent, "Share chat"))
    }
  }

  AskContent(
    isCompact = isCompact,
    contentPadding = contentPadding,
    consentAccepted = st.consentAccepted,
    threads = st.threads,
    messages = st.messages,
    sending = st.sending,
    error = st.error,
    draft = draft,
    onDraftChange = { draft = it },
    onOpenChats = { showChats = true },
    onNewChat = { vm.newChat() },
    onExportCurrent = { shareText(vm.exportCurrentThreadText()) },
    onClearCurrent = vm::clearCurrentThread,
    onDeleteCurrent = {
      val active = st.activeThreadId?.let { id -> st.threads.firstOrNull { it.id == id } }
      if (active != null) {
        deleteTarget = active
        showDeleteConfirm = true
      }
    },
    onSend = {
      // UX: hide keyboard and clear focus immediately on send (or consent dialog).
      focusManager.clearFocus(force = true)
      keyboard?.hide()
      val q = draft.trim()
      if (q.isBlank()) return@AskContent
      if (!st.consentAccepted) {
        showConsent = true
        return@AskContent
      }
      val locale = deviceLocale()
      vm.send(question = q, locale = locale)
      draft = ""
    },
    onAction = { action ->
      when (action.type.lowercase()) {
        "open_verify" -> onGoVerify()
        "open_help" -> onGoHelp()
        "open_official_url" -> {
          val url = action.payload?.url
          if (!url.isNullOrBlank()) openUrl(url)
        }
      }
    },
    onOpenCitation = { url -> openUrl(url) },
  )

  if (showChats) {
    ModalBottomSheet(
      onDismissRequest = { showChats = false },
    ) {
      ChatsSheet(
        threads = st.threads,
        activeThreadId = st.activeThreadId,
        onSelect = { tid -> vm.setActiveThread(tid); showChats = false },
        onNewChat = { vm.newChat(); showChats = false },
        onRename = { meta ->
          renameTarget = meta
          renameDraft = meta.title
          showRename = true
        },
        onDelete = { meta ->
          deleteTarget = meta
          showDeleteConfirm = true
        },
      )
    }
  }

  if (showRename && renameTarget != null) {
    AlertDialog(
      onDismissRequest = { showRename = false },
      confirmButton = {
        TextButton(
          onClick = {
            val meta = renameTarget ?: return@TextButton
            vm.renameThread(meta.id, renameDraft)
            showRename = false
          },
        ) { Text("Rename") }
      },
      dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } },
      title = { Text("Rename chat") },
      text = {
        CivicTextField(
          value = renameDraft,
          onValueChange = { renameDraft = it },
          label = "Title",
          placeholder = "Chat title",
          singleLine = true,
          testTag = "ask_rename_input",
        )
      },
    )
  }

  if (showDeleteConfirm && deleteTarget != null) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      confirmButton = {
        TextButton(
          onClick = {
            val meta = deleteTarget ?: return@TextButton
            vm.deleteThread(meta.id)
            Toast.makeText(context, "Chat deleted", Toast.LENGTH_SHORT).show()
            showDeleteConfirm = false
          },
        ) { Text("Delete") }
      },
      dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
      title = { Text("Delete chat?") },
      text = { Text("This deletes the chat from this device. This can't be undone.") },
    )
  }

  if (showConsent) {
    AlertDialog(
      onDismissRequest = { showConsent = false },
      confirmButton = {
        TextButton(
          onClick = {
            vm.acceptConsent()
            showConsent = false
            val q = draft.trim()
            if (q.isNotBlank()) {
              vm.send(question = q, locale = deviceLocale())
              draft = ""
            }
          },
        ) {
          Text(text = stringResource(R.string.ask_consent_agree))
        }
      },
      dismissButton = {
        TextButton(onClick = { showConsent = false }) {
          Text(text = stringResource(R.string.ask_consent_cancel))
        }
      },
      title = { Text(text = stringResource(R.string.ask_consent_title)) },
      text = { Text(text = stringResource(R.string.ask_consent_body)) },
    )
  }
}

@Composable
fun AskContent(
  isCompact: Boolean,
  contentPadding: PaddingValues = PaddingValues(0.dp),
  consentAccepted: Boolean,
  threads: List<AskThreadMeta> = emptyList(),
  messages: List<AskStoredMessage>,
  sending: Boolean,
  error: String?,
  draft: String,
  onDraftChange: (String) -> Unit,
  onOpenChats: () -> Unit,
  onNewChat: () -> Unit,
  onExportCurrent: () -> Unit,
  onClearCurrent: () -> Unit,
  onDeleteCurrent: () -> Unit,
  onSend: () -> Unit,
  onAction: (AskSuggestedAction) -> Unit,
  onOpenCitation: (String) -> Unit,
) {
  val chromeTopPadding = contentPadding.calculateTopPadding()
  val chromeBottomPadding = contentPadding.calculateBottomPadding()
  val accent = LocalCivicAccent.current
  var headerMenuOpen by remember { mutableStateOf(false) }

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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          TextButton(
            onClick = onOpenChats,
            enabled = threads.isNotEmpty(),
            modifier = Modifier.testTag("ask_open_chats"),
          ) {
            Text(text = "Chats")
          }
          TextButton(
            onClick = onNewChat,
            modifier = Modifier.testTag("ask_new_chat"),
          ) {
            Text(text = "New chat")
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = { headerMenuOpen = true },
            modifier = Modifier.testTag("ask_more"),
          ) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More")
          }
          DropdownMenu(expanded = headerMenuOpen, onDismissRequest = { headerMenuOpen = false }) {
            DropdownMenuItem(
              text = { Text("Export") },
              onClick = { headerMenuOpen = false; onExportCurrent() },
              modifier = Modifier.testTag("ask_export_chat"),
            )
            DropdownMenuItem(
              text = { Text("Clear this chat") },
              onClick = { headerMenuOpen = false; onClearCurrent() },
            )
            DropdownMenuItem(
              text = { Text("Delete this chat") },
              onClick = { headerMenuOpen = false; onDeleteCurrent() },
            )
          }
        }
      }
    }

    item {
      HeroPanel(
        title = stringResource(R.string.ask_title),
        subtitle = stringResource(R.string.ask_subtitle),
        badge = stringResource(R.string.nav_ask),
      )
    }

    if (messages.isEmpty()) {
      item {
        CivicCard {
          Text(
            text = stringResource(R.string.ask_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    } else {
      itemsIndexed(messages) { idx, msg ->
        val isUser = msg.role.lowercase() == "user"
        val bubbleMaxWidth = if (isCompact) 560.dp else 520.dp
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .testTag("ask_msg_$idx"),
          horizontalArrangement =
            if (isUser) Arrangement.End else Arrangement.Start,
        ) {
          androidx.compose.material3.Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color =
              if (isUser) accent.accent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
            contentColor =
              if (isUser) accent.onAccent else MaterialTheme.colorScheme.onSurface,
            border =
              if (isUser) null
              else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
            tonalElevation = 0.dp,
            shadowElevation = if (isUser) 2.dp else 4.dp,
            modifier = Modifier.widthIn(max = bubbleMaxWidth),
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              if (!isUser) {
                MarkdownText(
                  markdown = msg.content,
                  style = MaterialTheme.typography.bodyMedium,
                  textColor = MaterialTheme.colorScheme.onSurface,
                  linkColor = accent.accent,
                  onOpenUrl = onOpenCitation,
                  modifier = Modifier.fillMaxWidth(),
                )
              } else {
                Text(
                  text = msg.content,
                  style = MaterialTheme.typography.bodyMedium,
                )
              }

              if (!isUser) {
                if (msg.citations.isNotEmpty()) {
                  Text(
                    text = stringResource(R.string.ask_sources_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (c in msg.citations.take(6)) {
                      CivicLinkButton(
                        text = c.title,
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        onClick = { onOpenCitation(c.url) },
                      )
                    }
                  }
                }

                if (msg.actions.isNotEmpty()) {
                  Text(
                    text = stringResource(R.string.ask_suggested_actions),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (a in msg.actions.take(6)) {
                      CivicLinkButton(
                        text = a.label,
                        onClick = { onAction(a) },
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    item {
      CivicCard {
        CivicTextField(
          value = draft,
          onValueChange = onDraftChange,
          label = stringResource(R.string.ask_input_label),
          placeholder = stringResource(R.string.ask_input_placeholder),
          singleLine = false,
          minLines = 3,
          maxLines = 6,
          modifier = Modifier.fillMaxWidth(),
          testTag = "ask_input",
        )

        if (!error.isNullOrBlank()) {
          Text(text = error, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("ask_error_text"))
        }

        CivicPrimaryButton(
          text = if (sending) stringResource(R.string.ask_sending) else stringResource(R.string.ask_send),
          icon = Icons.AutoMirrored.Filled.Send,
          onClick = onSend,
          enabled = !sending && draft.trim().isNotBlank(),
          modifier =
            Modifier
              .fillMaxWidth()
              .testTag("ask_send"),
        )

        Text(
          text =
            if (consentAccepted) stringResource(R.string.ask_disclaimer_inline)
            else stringResource(R.string.ask_disclaimer_inline_needs_consent),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = stringResource(R.string.ask_context_hint),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun ChatsSheet(
  threads: List<AskThreadMeta>,
  activeThreadId: String?,
  onSelect: (String) -> Unit,
  onNewChat: () -> Unit,
  onRename: (AskThreadMeta) -> Unit,
  onDelete: (AskThreadMeta) -> Unit,
) {
  var q by remember { mutableStateOf("") }
  val query = q.trim().lowercase(Locale.US)
  val filtered =
    if (query.isBlank()) {
      threads
    } else {
      threads.filter { t ->
        t.title.lowercase(Locale.US).contains(query) || t.lastPreview.lowercase(Locale.US).contains(query)
      }
    }
  val df = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

  Column(
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(text = "Chats", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
      CivicPrimaryButton(
        text = "New chat",
        onClick = onNewChat,
        modifier = Modifier.testTag("ask_sheet_new_chat"),
      )
    }

    CivicTextField(
      value = q,
      onValueChange = { q = it },
      label = "Search",
      placeholder = "Search chats",
      singleLine = true,
      trailingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
      testTag = "ask_thread_search",
    )

    if (filtered.isEmpty()) {
      CivicCard {
        Text(
          text = "No chats match your search.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      return@Column
    }

    for ((idx, t) in filtered.withIndex()) {
      CivicCard(
        modifier = Modifier.testTag("ask_thread_item_$idx"),
        contentPadding = PaddingValues(12.dp),
      ) {
        val selected = t.id == activeThreadId
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            Text(
              text = t.title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            )
            if (t.lastPreview.isNotBlank()) {
              Text(
                text = t.lastPreview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
              )
            }
            Text(
              text = "${df.format(java.util.Date(t.updatedAt))} · ${t.messageCount} msgs",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onSelect(t.id) }) { Text(if (selected) "Active" else "Open") }
            IconButton(onClick = { onRename(t) }) { Icon(Icons.Filled.Edit, contentDescription = "Rename") }
            IconButton(onClick = { onDelete(t) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
          }
        }
      }
    }
  }
}

private fun deviceLocale(): String {
  val lang = Locale.getDefault().language.lowercase()
  return when {
    lang.startsWith("es") -> "es"
    lang.startsWith("zh") -> "zh-Hans"
    else -> "en"
  }
}
