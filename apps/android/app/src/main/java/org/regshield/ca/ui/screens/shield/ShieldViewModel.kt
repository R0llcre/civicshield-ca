package org.regshield.ca.ui.screens.shield

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.regshield.ca.data.ShieldPrefsRepository

data class ShieldUiState(
  val selectedFlagIds: Set<String> = emptySet(),
  val completedStepIds: Set<String> = emptySet(),
)

@HiltViewModel
class ShieldViewModel @Inject constructor(
  private val repo: ShieldPrefsRepository,
) : ViewModel() {
  private val _state = MutableStateFlow(ShieldUiState())
  val state: StateFlow<ShieldUiState> = _state.asStateFlow()

  init {
    viewModelScope.launch {
      repo.selectedFlagIds
        .combine(repo.completedStepIds) { flags, steps ->
          ShieldUiState(selectedFlagIds = flags, completedStepIds = steps)
        }
        .collect { _state.value = it }
    }
  }

  fun setFlagSelected(id: String, selected: Boolean) {
    viewModelScope.launch {
      repo.setFlagSelected(id, selected)
    }
  }

  fun setStepCompleted(id: String, completed: Boolean) {
    viewModelScope.launch {
      repo.setStepCompleted(id, completed)
    }
  }

  fun clearAll() {
    viewModelScope.launch {
      repo.clearAll()
    }
  }
}

