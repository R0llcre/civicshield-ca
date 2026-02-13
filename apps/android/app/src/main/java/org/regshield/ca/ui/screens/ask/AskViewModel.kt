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
import org.regshield.ca.api.RegShieldApi
import org.regshield.ca.data.AskLocalStore
import org.regshield.ca.data.AskStoredMessage

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
        val apiMsgs =
          nextLocal
            .takeLast(20)
            .map { AskMessage(role = it.role, content = it.content) }
        val resp = api.ask(AskRequest(locale = locale, messages = apiMsgs))

        val body = resp.body()
        if (!resp.isSuccessful) {
          val msg = body?.error ?: "AI request failed. Please try again."
          _state.update { it.copy(error = msg) }
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

