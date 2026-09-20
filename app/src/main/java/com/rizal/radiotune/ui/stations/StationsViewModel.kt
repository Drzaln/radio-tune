package com.rizal.radiotune.ui.stations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizal.radiotune.data.model.Station
import com.rizal.radiotune.data.model.StationSort
import com.rizal.radiotune.data.repository.RadioRepository
import kotlinx.coroutines.Job
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
    val tag: String = "",
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
    private var tagJob: Job? = null
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

    fun onTagChange(tag: String) {
        _state.update { it.copy(tag = tag) }
        tagJob?.cancel()
        tagJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            load(reset = true)
        }
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
        loadJob = viewModelScope.launch {
            if (reset) {
                offset = 0
                _state.update { it.copy(isLoading = true, isLoadingMore = false, error = null, endReached = false) }
            } else {
                _state.update { it.copy(isLoadingMore = true, error = null) }
            }

            val current = _state.value
            runCatching {
                repository.getStations(
                    countryCode = countryCode,
                    query = current.query,
                    tag = current.tag,
                    sort = current.sort,
                    offset = offset,
                    limit = RadioRepository.PAGE_SIZE,
                )
            }
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
                            error = throwable.toUserMessage(),
                        )
                    }
                }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
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
