package com.rizal.radiotune.ui.countries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizal.radiotune.data.model.Country
import com.rizal.radiotune.data.repository.RadioRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CountriesUiState(
    val countries: List<Country> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val visibleCountries: List<Country>
        get() = if (query.isBlank()) {
            countries
        } else {
            val term = query.trim()
            countries.filter {
                it.name.contains(term, ignoreCase = true) || it.code.contains(term, ignoreCase = true)
            }
        }
}

class CountriesViewModel(
    private val repository: RadioRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CountriesUiState())
    val state: StateFlow<CountriesUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load(force = false)
    }

    fun load(force: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = it.countries.isEmpty(), error = null) }
            runCatching { repository.getCountries(forceRefresh = force) }
                .onSuccess { countries ->
                    _state.update { it.copy(countries = countries, isLoading = false, error = null) }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Could not load the country list",
                        )
                    }
                }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
    }
}
