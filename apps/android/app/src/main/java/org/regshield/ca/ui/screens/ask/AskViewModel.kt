package org.regshield.ca.ui.screens.ask

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.regshield.ca.api.AskMessage
import org.regshield.ca.api.AskRequest
import org.regshield.ca.api.AskResponse
import org.regshield.ca.api.RegShieldApi
import org.regshield.ca.data.AskLocalStore
import org.regshield.ca.data.AskStoredMessage
import retrofit2.Response

data class AskUiState(
  val consentAccepted: Boolean = false,
  val messages: List<AskStoredMessage> = emptyList(),
  val sending: Boolean = false,
  val error: String? = null,
)

@HiltViewModel
class AskViewModel @Inject constructor(
  private val api: RegShieldApi,
  private val store: AskLocalStore,
) : ViewModel() {
  private val _state = MutableStateFlow(AskUiState())
  val state: StateFlow<AskUiState> = _state.asStateFlow()

  init {
    viewModelScope.launch {
      store.consentAccepted.collect { accepted ->
        _state.update { it.copy(consentAccepted = accepted) }
      }
    }
    viewModelScope.launch {
      store.messages.collect { msgs ->
        _state.update { it.copy(messages = msgs) }
      }
    }
  }

  fun acceptConsent() {
    viewModelScope.launch { store.setConsentAccepted(true) }
  }

  fun clearChat() {
    viewModelScope.launch {
      store.clearChat()
      _state.update { it.copy(error = null) }
    }
  }

  fun send(question: String, locale: String) {
    val q = question.trim()
    if (q.isBlank()) return
    if (_state.value.sending) return

    viewModelScope.launch {
      _state.update { it.copy(sending = true, error = null) }

      val base = _state.value.messages
      val nextLocal = (base + AskStoredMessage(role = "user", content = q, ts = System.currentTimeMillis()))
        .takeLast(50)

      // Persist immediately so the user doesn't lose their question on crashes.
      store.setMessages(nextLocal)

      try {
        val apiMsgs = buildApiMessages(nextLocal)
        val resp = api.ask(AskRequest(locale = locale, messages = apiMsgs))

        val body = resp.body()
        if (!resp.isSuccessful) {
          _state.update { it.copy(error = errorMessageFrom(resp)) }
          return@launch
        }
        val answer = body?.answerMarkdown?.trim().orEmpty()
        if (answer.isBlank()) {
          _state.update { it.copy(error = "No answer generated. Please rephrase and try again.") }
          return@launch
        }

        val assistantMsg =
          AskStoredMessage(
            role = "assistant",
            content = answer,
            ts = System.currentTimeMillis(),
            citations = body?.citations ?: emptyList(),
            actions = body?.suggestedActions ?: emptyList(),
          )
        store.setMessages((nextLocal + assistantMsg).takeLast(50))
      } catch (e: Exception) {
        _state.update { it.copy(error = "AI request failed. Please try again.") }
      } finally {
        _state.update { it.copy(sending = false) }
      }
    }
  }
}

private fun buildApiMessages(local: List<AskStoredMessage>): List<AskMessage> {
  // Keep the server happy (limits) while preserving the most recent context.
  val maxMessages = 20
  val maxCharsPerMessage = 1800
  val maxTotalChars = 7600

  val picked = mutableListOf<AskMessage>()
  var remaining = maxTotalChars

  for (m in local.asReversed()) {
    if (picked.size >= maxMessages) break
    val role = m.role.trim()
    val content = m.content.trim()
    if (role.isBlank() || content.isBlank()) continue

    var clipped = if (content.length > maxCharsPerMessage) content.takeLast(maxCharsPerMessage) else content
    if (clipped.length > remaining) {
      if (remaining < 200) break
      clipped = clipped.takeLast(remaining)
    }

    picked.add(AskMessage(role = role, content = clipped))
    remaining -= clipped.length
    if (remaining <= 0) break
  }

  return picked.asReversed()
}

private fun extractJsonError(raw: String): String? {
  // Minimal parsing to avoid pulling in another dependency; server never echoes user content.
  val m = Regex(""""error"\s*:\s*"([^"]+)"""").find(raw) ?: return null
  return m.groupValues.getOrNull(1)
    ?.replace("\\n", "\n")
    ?.replace("\\\"", "\"")
    ?.trim()
    ?.takeIf { it.isNotBlank() }
}

private fun errorMessageFrom(resp: Response<AskResponse>): String {
  val code = resp.code()
  val retryAfter = resp.headers()["retry-after"]?.toIntOrNull()
  if (code == 429 && retryAfter != null) {
    return "Rate limit exceeded. Please try again in ${retryAfter}s."
  }

  val raw = runCatching { resp.errorBody()?.string().orEmpty() }.getOrDefault("")
  val jsonErr = extractJsonError(raw)
  if (!jsonErr.isNullOrBlank()) return jsonErr

  return "AI request failed (HTTP $code). Please try again."
}
