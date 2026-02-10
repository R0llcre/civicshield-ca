package org.regshield.ca.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class VaultUiState(
  val hasVault: Boolean = false,
  val locked: Boolean = true,
  val busy: Boolean = false,
  val error: String? = null,
  val items: List<VaultItemMeta> = emptyList(),
)

@HiltViewModel
class VaultViewModel @Inject constructor(
  private val repo: VaultRepository,
) : ViewModel() {
  private val _state = MutableStateFlow(VaultUiState())
  val state: StateFlow<VaultUiState> = _state.asStateFlow()

  private var unlocked: VaultUnlocked? = null

  init {
    refreshHasVault()
  }

  fun refreshHasVault() {
    viewModelScope.launch {
      val has = withContext(Dispatchers.IO) { repo.hasVault() }
      _state.value = _state.value.copy(hasVault = has)
    }
  }

  fun lock() {
    unlocked = null
    _state.value = _state.value.copy(locked = true, items = emptyList(), error = null)
  }

  fun createNew(passphrase: String) {
    val pw = passphrase
    if (pw.isBlank()) {
      _state.value = _state.value.copy(error = "Enter a passphrase.")
      return
    }
    _state.value = _state.value.copy(busy = true, error = null)
    viewModelScope.launch {
      val res = runCatching {
        withContext(Dispatchers.IO) { repo.createNewVault(pw) }
      }
      res.onSuccess { u ->
        unlocked = u
        _state.value = VaultUiState(hasVault = true, locked = false, busy = false, items = u.items)
      }.onFailure { e ->
        _state.value = _state.value.copy(busy = false, error = e.message ?: "Failed to create vault")
      }
    }
  }

  fun unlock(passphrase: String) {
    val pw = passphrase
    if (pw.isBlank()) {
      _state.value = _state.value.copy(error = "Enter your passphrase.")
      return
    }
    _state.value = _state.value.copy(busy = true, error = null)
    viewModelScope.launch {
      val res = runCatching {
        withContext(Dispatchers.IO) { repo.unlock(pw) }
      }
      res.onSuccess { u ->
        unlocked = u
        _state.value = VaultUiState(hasVault = true, locked = false, busy = false, items = u.items)
      }.onFailure { _ ->
        // Do not reveal metadata; keep message generic.
        _state.value = _state.value.copy(busy = false, error = "Unable to unlock (wrong passphrase or corrupted vault).")
      }
    }
  }

  fun addFiles(uris: List<android.net.Uri>) {
    val u = unlocked ?: run {
      _state.value = _state.value.copy(error = "Unlock the vault first.")
      return
    }
    if (uris.isEmpty()) return
    _state.value = _state.value.copy(busy = true, error = null)
    viewModelScope.launch {
      val res = runCatching {
        withContext(Dispatchers.IO) { repo.addFiles(u, uris) }
      }
      res.onSuccess { next ->
        unlocked = next
        _state.value = _state.value.copy(busy = false, items = next.items)
      }.onFailure { e ->
        _state.value = _state.value.copy(busy = false, error = e.message ?: "Failed to add files")
      }
    }
  }

  fun deleteItem(id: String) {
    val u = unlocked ?: return
    _state.value = _state.value.copy(busy = true, error = null)
    viewModelScope.launch {
      val res = runCatching {
        withContext(Dispatchers.IO) { repo.deleteItem(u, id) }
      }
      res.onSuccess { next ->
        unlocked = next
        _state.value = _state.value.copy(busy = false, items = next.items)
      }.onFailure { e ->
        _state.value = _state.value.copy(busy = false, error = e.message ?: "Failed to delete item")
      }
    }
  }

  suspend fun exportZipBytes(): ByteArray {
    val u = unlocked ?: throw IllegalStateException("Unlock the vault first")
    return withContext(Dispatchers.IO) { repo.exportZip(u) }
  }

  fun importZip(passphrase: String, zipBytes: ByteArray) {
    val pw = passphrase
    if (pw.isBlank()) {
      _state.value = _state.value.copy(error = "Enter the passphrase for this vault export.")
      return
    }
    _state.value = _state.value.copy(busy = true, error = null)
    viewModelScope.launch {
      val res = runCatching {
        withContext(Dispatchers.IO) { repo.importZip(pw, zipBytes) }
      }
      res.onSuccess { next ->
        unlocked = next
        _state.value = VaultUiState(hasVault = true, locked = false, busy = false, items = next.items)
      }.onFailure {
        _state.value = _state.value.copy(busy = false, error = "Import failed (wrong passphrase or invalid file).")
      }
    }
  }
}
