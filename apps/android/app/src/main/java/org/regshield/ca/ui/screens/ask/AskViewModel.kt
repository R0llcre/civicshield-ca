package org.regshield.ca.ui.screens.ask

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import org.regshield.ca.api.AskMessage
import org.regshield.ca.api.AskRequest
import org.regshield.ca.api.AskResponse
import org.regshield.ca.api.RegShieldApi
import org.regshield.ca.data.AskLocalStore
import org.regshield.ca.data.AskThreadMeta
import org.regshield.ca.data.AskStoredMessage
import retrofit2.Response

data class AskUiState(
  val consentAccepted: Boolean = false,
  val threads: List<AskThreadMeta> = emptyList(),
  val activeThreadId: String? = null,
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
      // One-time migration from the legacy single-chat storage.
      store.ensureMigratedToThreads()
      val idx = store.threadsIndex.first()
      if (idx.threads.isEmpty()) {
        store.createThread("Chat 1")
      } else if (idx.activeThreadId == null) {
        store.setActiveThread(idx.threads.last().id)
      }
    }
    viewModelScope.launch {
      store.consentAccepted.collect { accepted ->
        _state.update { it.copy(consentAccepted = accepted) }
      }
    }
    viewModelScope.launch {
      store.threadsIndex.collect { idx ->
        val active = idx.activeThreadId ?: idx.threads.lastOrNull()?.id
        _state.update { it.copy(threads = idx.threads, activeThreadId = active) }
      }
    }
    viewModelScope.launch {
      store.activeThreadMessages.collect { msgs ->
        val sanitized = sanitizeStoredMessages(msgs)
        // Cleanup: older builds may have stored roles like "User"/"Assistant" which the server rejects.
        val activeId = _state.value.activeThreadId
        if (activeId != null && sanitized != msgs) {
          store.setThreadMessages(activeId, sanitized)
        }
        _state.update { it.copy(messages = sanitized) }
      }
    }
  }

  fun newChat(title: String? = null) {
    viewModelScope.launch {
      store.createThread(title)
    }
  }

  fun setActiveThread(threadId: String) {
    viewModelScope.launch { store.setActiveThread(threadId) }
  }

  fun renameThread(threadId: String, newTitle: String) {
    viewModelScope.launch { store.renameThread(threadId, newTitle) }
  }

  fun deleteThread(threadId: String) {
    viewModelScope.launch { store.deleteThread(threadId) }
  }

  fun clearCurrentThread() {
    val tid = _state.value.activeThreadId ?: return
    viewModelScope.launch {
      store.clearActiveThread(tid)
      _state.update { it.copy(error = null) }
    }
  }

  fun exportCurrentThreadText(): String {
    val threads = _state.value.threads.associateBy { it.id }
    val tid = _state.value.activeThreadId
    val title = tid?.let { threads[it]?.title } ?: "Chat"
    return exportThreadText(title = title, messages = _state.value.messages)
  }

  fun acceptConsent() {
    viewModelScope.launch { store.setConsentAccepted(true) }
  }

  fun clearChat() {
    clearCurrentThread()
  }

  fun send(question: String, locale: String) {
    val q = question.trim()
    if (q.isBlank()) return
    if (_state.value.sending) return

    viewModelScope.launch {
      _state.update { it.copy(sending = true, error = null) }

      val threadId = _state.value.activeThreadId ?: store.createThread("Chat 1")
      store.setActiveThread(threadId)

      val base = _state.value.messages
      val nextLocal =
        (base + AskStoredMessage(role = "user", content = q, ts = System.currentTimeMillis()))
          .takeLast(50)

      // Persist immediately so the user doesn't lose their question on crashes.
      store.setThreadMessages(threadId, nextLocal)

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
        store.setThreadMessages(threadId, (nextLocal + assistantMsg).takeLast(50))
      } catch (e: Exception) {
        val msg =
          when (e) {
            is SocketTimeoutException -> "Request timed out. Please try again."
            else -> "AI request failed. Please try again."
          }
        _state.update { it.copy(error = msg) }
      } finally {
        _state.update { it.copy(sending = false) }
      }
    }
  }
}

private fun exportThreadText(title: String, messages: List<AskStoredMessage>): String {
  fun fmtTs(ts: Long): String {
    // Keep it simple; avoid java.time (minSdk 24). UTC ISO-ish timestamp.
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
    return sdf.format(java.util.Date(ts))
  }

  val sb = StringBuilder()
  sb.appendLine(title)
  sb.appendLine("Information only. Not legal advice.")
  sb.appendLine()
  for (m in messages) {
    val role = m.role.trim().lowercase()
    if (role != "user" && role != "assistant") continue
    sb.appendLine("[${fmtTs(m.ts)}] ${role}:")
    sb.appendLine(m.content.trim())
    sb.appendLine()
    if (role == "assistant" && m.citations.isNotEmpty()) {
      sb.appendLine("Sources:")
      for (c in m.citations.take(6)) {
        sb.appendLine("- ${c.title}: ${c.url}")
      }
      sb.appendLine()
    }
  }
  return sb.toString().trim()
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
    val role =
      when (m.role.trim().lowercase()) {
        "user" -> "user"
        "assistant" -> "assistant"
        else -> continue
      }
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

private fun sanitizeStoredMessages(raw: List<AskStoredMessage>): List<AskStoredMessage> {
  // Keep only valid roles and non-blank content; normalize role casing.
  val out = ArrayList<AskStoredMessage>(raw.size)
  for (m in raw) {
    val role =
      when (m.role.trim().lowercase()) {
        "user" -> "user"
        "assistant" -> "assistant"
        else -> continue
      }
    val content = m.content.trim()
    if (content.isBlank()) continue
    out.add(m.copy(role = role, content = content))
  }
  return if (out.size <= 50) out else out.takeLast(50)
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
