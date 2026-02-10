package org.regshield.ca.ui.screens.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.regshield.ca.api.RaEntry
import org.regshield.ca.data.VerifyRepository

data class VerifyUiState(
  val loading: Boolean = false,
  val error: String? = null,
  val dataFreshnessEndedAt: String? = null,
  val results: List<RaEntry> = emptyList(),
)

@HiltViewModel
class VerifyViewModel @Inject constructor(
  private val repo: VerifyRepository,
) : ViewModel() {
  private val _state = MutableStateFlow(VerifyUiState())
  val state: StateFlow<VerifyUiState> = _state.asStateFlow()

  fun search(name: String, state: String) {
    val trimmedName = name.trim()
    val trimmedState = state.trim().uppercase().ifBlank { "CA" }
    if (trimmedName.isBlank()) {
      _state.value = _state.value.copy(error = "Please enter a name.")
      return
    }

    _state.value = VerifyUiState(loading = true)
    viewModelScope.launch {
      val res = repo.verify(trimmedName, trimmedState)
      _state.value = res.fold(
        onSuccess = { body ->
          VerifyUiState(
            loading = false,
            error = body.error,
            dataFreshnessEndedAt = body.dataFreshness?.endedAt,
            results = body.results,
          )
        },
        onFailure = { err ->
          VerifyUiState(loading = false, error = err.message ?: "Request failed")
        },
      )
    }
  }

  fun clearError() {
    _state.value = _state.value.copy(error = null)
  }
}

