package com.rizal.radiotune.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizal.radiotune.BuildConfig
import com.rizal.radiotune.data.update.UpdateCheckResult
import com.rizal.radiotune.data.update.UpdateInfo
import com.rizal.radiotune.data.update.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UpdateUiState(
    val checking: Boolean = false,
    val update: UpdateInfo? = null,
    val downloading: Boolean = false,
    val progress: Int = 0,
    val error: String? = null,
    val message: String? = null,
)

class UpdateViewModel(
    private val repository: UpdateRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    fun checkOnLaunch() {
        viewModelScope.launch { check(force = false) }
    }

    fun checkNow() {
        viewModelScope.launch { check(force = true) }
    }

    fun install() {
        val update = _state.value.update ?: return
        if (_state.value.downloading) return

        viewModelScope.launch {
            _state.update { it.copy(downloading = true, progress = 0, error = null) }
            runCatching {
                repository.download(update) { percent ->
                    _state.update { it.copy(progress = percent) }
                }
            }.onSuccess { apk ->
                _state.update { it.copy(downloading = false, update = null) }
                runCatching { repository.install(apk) }
                    .onFailure {
                        _state.update { it.copy(error = "Could not open the package installer") }
                    }
            }.onFailure { error ->
                _state.update {
                    it.copy(downloading = false, error = error.message ?: "Download failed")
                }
            }
        }
    }

    fun dismiss() {
        _state.update { it.copy(update = null, error = null) }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null, error = null) }
    }

    private suspend fun check(force: Boolean) {
        _state.update { it.copy(checking = true, message = null, error = null) }
        when (val result = repository.checkForUpdate(BuildConfig.VERSION_CODE, force)) {
            is UpdateCheckResult.Available ->
                _state.update { it.copy(checking = false, update = result.update) }

            UpdateCheckResult.UpToDate ->
                _state.update {
                    it.copy(
                        checking = false,
                        message = if (force) "RadioTune is up to date" else null,
                    )
                }

            UpdateCheckResult.Skipped ->
                _state.update { it.copy(checking = false) }

            is UpdateCheckResult.Failed ->
                _state.update {
                    it.copy(checking = false, error = if (force) result.message else null)
                }
        }
    }
}
