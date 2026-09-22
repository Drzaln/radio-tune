package com.rizal.radiotune.ui.stations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizal.radiotune.data.model.Station
import com.rizal.radiotune.data.model.StationSort
import com.rizal.radiotune.data.repository.RadioRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class StationsUiState(
    val stations: List<Station> = emptyList(),
    val query: String = "",
    val selectedTags: List<String> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val sort: StationSort = StationSort.POPULARITY,
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
    private var requestId = 0
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

    fun onTagToggle(tag: String) {
        val selected = _state.value.selectedTags
        val updated = if (tag in selected) selected - tag else selected + tag
        _state.update { it.copy(selectedTags = updated) }
        load(reset = true)
    }

    fun clearTags() {
        if (_state.value.selectedTags.isEmpty()) return
        _state.update { it.copy(selectedTags = emptyList()) }
        load(reset = true)
    }

    fun onSortChange(sort: StationSort) {
        if (_state.value.sort == sort) return
        _state.update { it.copy(sort = sort) }
        load(reset = true)
    }

    fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || current.endReached) return
        load(reset = false)
    }

    fun retry() = load(reset = true)

    private fun load(reset: Boolean) {
        loadJob?.cancel()
        val token = ++requestId
        loadJob = viewModelScope.launch {
            if (reset) {
                offset = 0
                _state.update { it.copy(isLoading = true, isLoadingMore = false, error = null, endReached = false) }
            } else {
                _state.update { it.copy(isLoadingMore = true, error = null) }
            }

            val current = _state.value
            val result = runCatching {
                repository.getStations(
                    countryCode = countryCode,
                    query = current.query,
                    tags = current.selectedTags,
                    sort = current.sort,
                    offset = offset,
                    limit = RadioRepository.PAGE_SIZE,
                )
            }

            // A newer load (or a cancelled one) must not touch the state: its page
            // would otherwise be appended to a different query and repeat ids.
            if (token != requestId) return@launch

            result
                .onSuccess { page ->
                    offset += page.rawSize
                    val pageTags = page.stations.flatMap { it.tags }
                        .map { it.trim().lowercase() }
                        .filter { it.isNotEmpty() }
                        .groupingBy { it }
                        .eachCount()
                        .entries
                        .sortedByDescending { it.value }
                        .map { it.key }
                    _state.update { state ->
                        state.copy(
                            stations = if (reset) {
                                page.stations
                            } else {
                                (state.stations + page.stations).distinctBy { it.id }
                            },
                            // Only grow the picker while unfiltered, otherwise it
                            // would shrink to the tags of the filtered results.
                            availableTags = if (state.selectedTags.isEmpty()) {
                                if (reset) {
                                    pageTags.take(MAX_TAGS)
                                } else {
                                    (state.availableTags + pageTags).distinct().take(MAX_TAGS)
                                }
                            } else {
                                state.availableTags
                            },
                            isLoading = false,
                            isLoadingMore = false,
                            endReached = page.rawSize < RadioRepository.PAGE_SIZE,
                            error = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    if (throwable is CancellationException) return@launch
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = throwable.toUserMessage(),
                        )
                    }
                }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
        const val MAX_TAGS = 24
    }
}

private fun Throwable.toUserMessage(): String = when (this) {
    is UnknownHostException,
    is ConnectException,
    is SocketTimeoutException,
    -> "Network problem — check your connection"

    is IOException -> "Could not reach the station directory"

    else -> "Could not load stations"
}
