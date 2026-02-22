package org.regshield.ca.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.regshield.ca.api.AskCitation
import org.regshield.ca.api.AskSuggestedAction

private val Context.askDataStore by preferencesDataStore(name = "ask_prefs")

private const val THREADS_VERSION = 2
private const val MAX_THREADS = 30
private const val MAX_MESSAGES_PER_THREAD = 50
private const val MAX_PREVIEW_CHARS = 80

@Serializable
data class AskStoredMessage(
  val role: String,
  val content: String,
  val ts: Long,
  val citations: List<AskCitation> = emptyList(),
  val actions: List<AskSuggestedAction> = emptyList(),
)

@Serializable
data class AskThreadMeta(
  val id: String,
  val title: String,
  val createdAt: Long,
  val updatedAt: Long,
  val messageCount: Int,
  val lastPreview: String,
)

@Serializable
data class AskThreadsIndex(
  val version: Int = THREADS_VERSION,
  val activeThreadId: String? = null,
  val threads: List<AskThreadMeta> = emptyList(),
)

internal fun capAskMessages(next: List<AskStoredMessage>): List<AskStoredMessage> {
  return if (next.size <= MAX_MESSAGES_PER_THREAD) next else next.takeLast(MAX_MESSAGES_PER_THREAD)
}

internal fun buildAskThreadMeta(threadId: String, existingTitle: String?, messages: List<AskStoredMessage>, nowMs: Long): AskThreadMeta {
  fun condense(s: String): String = s.trim().replace(Regex("\\s+"), " ")

  val createdAt = messages.firstOrNull()?.ts ?: nowMs
  val updatedAt = messages.lastOrNull()?.ts ?: createdAt
  val lastPreview = condense(messages.lastOrNull()?.content.orEmpty()).take(MAX_PREVIEW_CHARS)

  val titleFromFirstUser =
    messages.firstOrNull { it.role.trim().lowercase() == "user" }?.content
      ?.let { condense(it) }
      ?.take(48)
      ?.takeUnless { it.isBlank() }

  val title = (existingTitle?.trim().takeUnless { it.isNullOrBlank() } ?: titleFromFirstUser ?: "Chat 1").take(48)

  return AskThreadMeta(
    id = threadId,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
    messageCount = messages.size,
    lastPreview = lastPreview,
  )
}

class AskLocalStore @Inject constructor(
  private val context: Context,
  private val json: Json,
) {
  private val dataStore = context.askDataStore

  private val keyConsent = booleanPreferencesKey("consent_accepted")
  // v1 single-chat storage (legacy)
  private val keyChatV1 = stringPreferencesKey("chat_json")
  // v2 multi-thread storage
  private val keyThreadsIndex = stringPreferencesKey("threads_index_json")

  private fun keyThreadMessages(threadId: String) = stringPreferencesKey("thread_${threadId}_messages_json")

  val consentAccepted: Flow<Boolean> =
    dataStore.data.map { prefs: Preferences -> prefs[keyConsent] ?: false }

  val threadsIndex: Flow<AskThreadsIndex> =
    dataStore.data.map { prefs ->
      decodeThreadsIndex(prefs[keyThreadsIndex]) ?: AskThreadsIndex()
    }

  val activeThreadId: Flow<String?> =
    threadsIndex.map { idx ->
      idx.activeThreadId ?: idx.threads.lastOrNull()?.id
    }

  val activeThreadMessages: Flow<List<AskStoredMessage>> =
    dataStore.data.map { prefs ->
      val idx = decodeThreadsIndex(prefs[keyThreadsIndex]) ?: return@map emptyList()
      val activeId = idx.activeThreadId ?: idx.threads.lastOrNull()?.id ?: return@map emptyList()
      decodeMessages(prefs[keyThreadMessages(activeId)]) ?: emptyList()
    }

  val threads: Flow<List<AskThreadMeta>> = threadsIndex.map { it.threads }

  suspend fun setConsentAccepted(accepted: Boolean) {
    dataStore.edit { prefs -> prefs[keyConsent] = accepted }
  }

  suspend fun ensureMigratedToThreads() {
    dataStore.edit { prefs ->
      if (prefs[keyThreadsIndex] != null) return@edit

      val rawOld = prefs[keyChatV1] ?: return@edit
      val oldMsgs = decodeMessages(rawOld) ?: emptyList()
      if (oldMsgs.isEmpty()) return@edit

      val capped = capAskMessages(oldMsgs)
      val threadId = UUID.randomUUID().toString()
      val meta = buildAskThreadMeta(threadId = threadId, existingTitle = null, messages = capped, nowMs = System.currentTimeMillis())
      val idx = AskThreadsIndex(activeThreadId = threadId, threads = listOf(meta))

      prefs[keyThreadsIndex] = json.encodeToString(idx)
      prefs[keyThreadMessages(threadId)] = json.encodeToString(capped)
      // Clear legacy storage so we don't re-migrate.
      prefs[keyChatV1] = "[]"
    }
  }

  suspend fun createThread(initialTitle: String? = null): String {
    val id = UUID.randomUUID().toString()
    dataStore.edit { prefs ->
      val now = System.currentTimeMillis()
      val existing = decodeThreadsIndex(prefs[keyThreadsIndex]) ?: AskThreadsIndex()
      val title = (initialTitle?.trim().takeUnless { it.isNullOrBlank() } ?: "New chat").take(48)
      val meta =
        AskThreadMeta(
          id = id,
          title = title,
          createdAt = now,
          updatedAt = now,
          messageCount = 0,
          lastPreview = "",
        )

      val nextThreads = (existing.threads + meta)
        .sortedBy { it.createdAt }
        .let { t -> if (t.size <= MAX_THREADS) t else t.takeLast(MAX_THREADS) }

      // Evict oldest threads if we exceeded the cap.
      val evicted = existing.threads.map { it.id }.toSet() - nextThreads.map { it.id }.toSet()
      for (tid in evicted) {
        prefs.remove(keyThreadMessages(tid))
      }

      val nextIdx = existing.copy(activeThreadId = id, threads = nextThreads)
      prefs[keyThreadsIndex] = json.encodeToString(nextIdx)
      prefs[keyThreadMessages(id)] = "[]"
    }
    return id
  }

  suspend fun setActiveThread(threadId: String) {
    dataStore.edit { prefs ->
      val existing = decodeThreadsIndex(prefs[keyThreadsIndex]) ?: AskThreadsIndex()
      if (existing.threads.none { it.id == threadId }) return@edit
      val nextIdx = existing.copy(activeThreadId = threadId)
      prefs[keyThreadsIndex] = json.encodeToString(nextIdx)
    }
  }

  suspend fun renameThread(threadId: String, newTitle: String) {
    val title = newTitle.trim().take(48)
    if (title.isBlank()) return
    dataStore.edit { prefs ->
      val existing = decodeThreadsIndex(prefs[keyThreadsIndex]) ?: AskThreadsIndex()
      val nextThreads =
        existing.threads.map { m ->
          if (m.id == threadId) m.copy(title = title, updatedAt = System.currentTimeMillis()) else m
        }
      prefs[keyThreadsIndex] = json.encodeToString(existing.copy(threads = nextThreads))
    }
  }

  suspend fun deleteThread(threadId: String) {
    dataStore.edit { prefs ->
      val existing = decodeThreadsIndex(prefs[keyThreadsIndex]) ?: AskThreadsIndex()
      if (existing.threads.none { it.id == threadId }) return@edit
      val nextThreads = existing.threads.filterNot { it.id == threadId }
      val nextActive =
        if (existing.activeThreadId == threadId) nextThreads.lastOrNull()?.id else existing.activeThreadId
      prefs.remove(keyThreadMessages(threadId))
      prefs[keyThreadsIndex] = json.encodeToString(existing.copy(activeThreadId = nextActive, threads = nextThreads))
    }
  }

  suspend fun setThreadMessages(threadId: String, next: List<AskStoredMessage>) {
    val capped = capAskMessages(next)
    dataStore.edit { prefs ->
      val existing = decodeThreadsIndex(prefs[keyThreadsIndex]) ?: AskThreadsIndex()
      val idx = existing.threads.indexOfFirst { it.id == threadId }
      if (idx < 0) return@edit

      prefs[keyThreadMessages(threadId)] = json.encodeToString(capped)

      val meta = buildAskThreadMeta(threadId = threadId, existingTitle = existing.threads[idx].title, messages = capped, nowMs = System.currentTimeMillis())
      val nextThreads = existing.threads.toMutableList().apply { this[idx] = meta }
      prefs[keyThreadsIndex] = json.encodeToString(existing.copy(threads = nextThreads, activeThreadId = threadId))
    }
  }

  suspend fun clearActiveThread(threadId: String) {
    setThreadMessages(threadId, emptyList())
  }

  suspend fun clearAllThreads() {
    dataStore.edit { prefs ->
      val existing = decodeThreadsIndex(prefs[keyThreadsIndex]) ?: AskThreadsIndex()
      for (t in existing.threads) {
        prefs.remove(keyThreadMessages(t.id))
      }
      prefs[keyThreadsIndex] = json.encodeToString(AskThreadsIndex())
    }
  }

  private fun decodeThreadsIndex(raw: String?): AskThreadsIndex? {
    if (raw.isNullOrBlank()) return null
    return try {
      val idx = json.decodeFromString<AskThreadsIndex>(raw)
      if (idx.version != THREADS_VERSION) null else idx
    } catch (_: Exception) {
      null
    }
  }

  private fun decodeMessages(raw: String?): List<AskStoredMessage>? {
    if (raw.isNullOrBlank()) return null
    return try {
      json.decodeFromString<List<AskStoredMessage>>(raw)
    } catch (_: Exception) {
      null
    }
  }

}
