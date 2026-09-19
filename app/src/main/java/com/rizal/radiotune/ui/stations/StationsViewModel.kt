package com.rizal.radiotune.ui.stations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizal.radiotune.data.model.Station
import com.rizal.radiotune.data.repository.RadioRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StationsUiState(
    val stations: List<Station> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null,
)

class StationsViewModel(
    private val repository: RadioRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val countryCode: String = savedStateHandle.get<String>("code").orEmpty()
    val countryName: String = savedStateHandle.get<String>("name").orEmpty()
        .ifBlank { countryCode.uppercase() }

    private val _state = MutableStateFlow(StationsUiState())
    val state: StateFlow<StationsUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var queryJob: Job? = null
    private var offset = 0

    init {
        load(reset = true)
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        queryJob?.cancel()
        queryJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            load(reset = true)
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || current.endReached) return
        load(reset = false)
    }

    fun retry() = load(reset = true)

    private fun load(reset: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (reset) {
                offset = 0
                _state.update { it.copy(isLoading = true, isLoadingMore = false, error = null, endReached = false) }
            } else {
                _state.update { it.copy(isLoadingMore = true, error = null) }
            }

            val query = _state.value.query
            runCatching { repository.getStations(countryCode, query, offset, RadioRepository.PAGE_SIZE) }
                .onSuccess { page ->
                    offset += page.size
                    _state.update { current ->
                        current.copy(
                            stations = if (reset) page else current.stations + page,
                            isLoading = false,
                            isLoadingMore = false,
                            endReached = page.size < RadioRepository.PAGE_SIZE,
                            error = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = throwable.message ?: "Could not load stations",
                        )
                    }
                }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}
