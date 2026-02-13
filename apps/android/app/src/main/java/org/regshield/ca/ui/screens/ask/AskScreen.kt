package org.regshield.ca.ui.screens.ask

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Locale
import org.regshield.ca.R
import org.regshield.ca.api.AskSuggestedAction
import org.regshield.ca.data.AskStoredMessage
import org.regshield.ca.ui.components.CivicCard
import org.regshield.ca.ui.components.CivicLinkButton
import org.regshield.ca.ui.components.CivicPrimaryButton
import org.regshield.ca.ui.components.CivicTextField
import org.regshield.ca.ui.components.HeroPanel
import org.regshield.ca.ui.theme.LocalCivicAccent

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
  val context = LocalContext.current

  fun openUrl(url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
  }

  AskContent(
    isCompact = isCompact,
    contentPadding = contentPadding,
    consentAccepted = st.consentAccepted,
    messages = st.messages,
    sending = st.sending,
    error = st.error,
    draft = draft,
    onDraftChange = { draft = it },
    onClearChat = vm::clearChat,
    onSend = {
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
  messages: List<AskStoredMessage>,
  sending: Boolean,
  error: String?,
  draft: String,
  onDraftChange: (String) -> Unit,
  onClearChat: () -> Unit,
  onSend: () -> Unit,
  onAction: (AskSuggestedAction) -> Unit,
  onOpenCitation: (String) -> Unit,
) {
  val topPadding = contentPadding.calculateTopPadding()
  val bottomPadding = contentPadding.calculateBottomPadding()
  val accent = LocalCivicAccent.current

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
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
      ) {
        TextButton(
          onClick = onClearChat,
          enabled = messages.isNotEmpty(),
          modifier = Modifier.testTag("ask_clear_chat"),
        ) {
          Text(text = stringResource(R.string.ask_clear_chat))
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
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .testTag("ask_msg_$idx"),
          horizontalArrangement =
            if (msg.role.lowercase() == "user") Arrangement.End else Arrangement.Start,
        ) {
          androidx.compose.material3.Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color =
              if (msg.role.lowercase() == "user") accent.accent else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            contentColor =
              if (msg.role.lowercase() == "user") accent.onAccent else MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(if (isCompact) 0.92f else 0.7f),
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text(
                text = msg.content,
                style = MaterialTheme.typography.bodyMedium,
              )

              if (msg.role.lowercase() == "assistant") {
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
