package org.regshield.ca.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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

@Serializable
data class AskStoredMessage(
  val role: String,
  val content: String,
  val ts: Long,
  val citations: List<AskCitation> = emptyList(),
  val actions: List<AskSuggestedAction> = emptyList(),
)

class AskLocalStore @Inject constructor(
  private val context: Context,
  private val json: Json,
) {
  private val dataStore = context.askDataStore

  private val keyConsent = booleanPreferencesKey("consent_accepted")
  private val keyChat = stringPreferencesKey("chat_json")

  val consentAccepted: Flow<Boolean> =
    dataStore.data.map { prefs: Preferences -> prefs[keyConsent] ?: false }

  val messages: Flow<List<AskStoredMessage>> =
    dataStore.data.map { prefs: Preferences ->
      val raw = prefs[keyChat] ?: return@map emptyList()
      try {
        json.decodeFromString<List<AskStoredMessage>>(raw)
      } catch (e: Exception) {
        emptyList()
      }
    }

  suspend fun setConsentAccepted(accepted: Boolean) {
    dataStore.edit { prefs -> prefs[keyConsent] = accepted }
  }

  suspend fun setMessages(next: List<AskStoredMessage>) {
    val capped = if (next.size <= 50) next else next.takeLast(50)
    val raw = json.encodeToString(capped)
    dataStore.edit { prefs -> prefs[keyChat] = raw }
  }

  suspend fun clearChat() {
    dataStore.edit { prefs -> prefs[keyChat] = "[]" }
  }
}
