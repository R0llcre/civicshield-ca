package org.regshield.ca.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.shieldDataStore by preferencesDataStore(name = "shield_prefs")

class ShieldPrefsRepository(
  private val context: Context,
) {
  private val dataStore = context.shieldDataStore

  private val keyFlags = stringSetPreferencesKey("selected_flags")
  private val keySteps = stringSetPreferencesKey("completed_steps")

  val selectedFlagIds: Flow<Set<String>> =
    dataStore.data.map { prefs: Preferences -> prefs[keyFlags] ?: emptySet() }

  val completedStepIds: Flow<Set<String>> =
    dataStore.data.map { prefs: Preferences -> prefs[keySteps] ?: emptySet() }

  suspend fun setFlagSelected(id: String, selected: Boolean) {
    if (id.isBlank()) return
    dataStore.edit { prefs ->
      val current = prefs[keyFlags] ?: emptySet()
      prefs[keyFlags] =
        if (selected) current + id else current - id
    }
  }

  suspend fun setStepCompleted(id: String, completed: Boolean) {
    if (id.isBlank()) return
    dataStore.edit { prefs ->
      val current = prefs[keySteps] ?: emptySet()
      prefs[keySteps] =
        if (completed) current + id else current - id
    }
  }

  suspend fun clearAll() {
    dataStore.edit { prefs ->
      prefs[keyFlags] = emptySet()
      prefs[keySteps] = emptySet()
    }
  }
}

