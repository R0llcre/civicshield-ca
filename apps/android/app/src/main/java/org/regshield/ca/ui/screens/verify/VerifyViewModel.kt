package org.regshield.ca.ui.screens.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.regshield.ca.R
import org.regshield.ca.api.RaEntry
import org.regshield.ca.data.VerifyRepository

data class VerifyUiState(
  val hasSearched: Boolean = false,
  val queryName: String? = null,
  val queryState: String? = null,
  val loading: Boolean = false,
  val errorResId: Int? = null,
  val errorMessage: String? = null,
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
      _state.value =
        _state.value.copy(
          errorResId = R.string.verify_error_enter_name,
          errorMessage = null,
        )
      return
    }

    // Mark this request, keep old results during loading to avoid flashing.
    _state.value =
      _state.value.copy(
        loading = true,
        errorResId = null,
        errorMessage = null,
        queryName = trimmedName,
        queryState = trimmedState,
      )
    viewModelScope.launch {
      val res = repo.verify(trimmedName, trimmedState)
      _state.value = res.fold(
        onSuccess = { body ->
          VerifyUiState(
            hasSearched = true,
            queryName = trimmedName,
            queryState = trimmedState,
            loading = false,
            errorResId = null,
            errorMessage = body.error,
            dataFreshnessEndedAt = body.dataFreshness?.endedAt,
            results = body.results,
          )
        },
        onFailure = { err ->
          _state.value.copy(
            hasSearched = true,
            loading = false,
            errorResId = null,
            errorMessage = err.message ?: "Request failed",
          )
        },
      )
    }
  }

  fun clearError() {
    _state.value = _state.value.copy(errorResId = null, errorMessage = null)
  }
}
