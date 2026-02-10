package org.regshield.ca.ui.screens.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.regshield.ca.api.AidOrg
import org.regshield.ca.data.DirectoryRepository

data class HelpFilters(
  val county: String = "",
  val language: String = "",
  val service: String = "",
)

data class HelpUiState(
  val loading: Boolean = false,
  val error: String? = null,
  val filters: HelpFilters = HelpFilters(),
  val results: List<AidOrg> = emptyList(),
)

@HiltViewModel
class HelpViewModel @Inject constructor(
  private val repo: DirectoryRepository,
) : ViewModel() {
  private val _state = MutableStateFlow(HelpUiState())
  val state: StateFlow<HelpUiState> = _state.asStateFlow()

  fun setCounty(v: String) {
    _state.value = _state.value.copy(filters = _state.value.filters.copy(county = v))
  }

  fun setLanguage(v: String) {
    _state.value = _state.value.copy(filters = _state.value.filters.copy(language = v))
  }

  fun setService(v: String) {
    _state.value = _state.value.copy(filters = _state.value.filters.copy(service = v))
  }

  fun search() {
    val f = _state.value.filters
    _state.value = _state.value.copy(loading = true, error = null)
    viewModelScope.launch {
      val res = repo.search(
        county = f.county.ifBlank { null },
        lang = f.language.ifBlank { null },
        service = f.service.ifBlank { null },
      )
      _state.value = res.fold(
        onSuccess = { body ->
          val sorted = body.results.sortedWith(compareBy<AidOrg>({ statusRank(it.intakeStatus) }, { it.name }))
          _state.value.copy(loading = false, results = sorted, error = body.error)
        },
        onFailure = { err ->
          _state.value.copy(loading = false, error = err.message ?: "Request failed")
        },
      )
    }
  }

  private fun statusRank(s: String?): Int = when (s) {
    "open" -> 0
    "waitlist" -> 1
    "closed" -> 2
    else -> 9
  }
}

