package com.rizal.radiotune.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizal.radiotune.data.model.Station
import com.rizal.radiotune.data.repository.FavoritesRepository
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val repository: FavoritesRepository,
) : ViewModel() {

    val favorites: StateFlow<List<Station>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun move(from: Int, to: Int) {
        viewModelScope.launch { repository.move(from, to) }
    }

    fun exportTo(output: OutputStream) {
        viewModelScope.launch {
            runCatching {
                output.use { it.write(repository.exportJson().toByteArray()) }
            }
        }
    }

    fun importFrom(input: InputStream) {
        viewModelScope.launch {
            runCatching {
                val raw = input.use { it.readBytes().decodeToString() }
                repository.import(raw)
            }
        }
    }
}
