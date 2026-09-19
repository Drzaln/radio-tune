package com.rizal.radiotune.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rizal.radiotune.data.model.Station
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class FavoritesRepository(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {

    val favorites: Flow<List<Station>> = dataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { preferences -> decode(preferences[FAVORITES_KEY]) }
        .distinctUntilChanged()

    suspend fun toggle(station: Station) {
        dataStore.edit { preferences ->
            val current = decode(preferences[FAVORITES_KEY])
            val updated = if (current.any { it.id == station.id }) {
                current.filterNot { it.id == station.id }
            } else {
                listOf(station) + current
            }
            preferences[FAVORITES_KEY] = json.encodeToString(updated)
        }
    }

    private fun decode(raw: String?): List<Station> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<Station>>(raw) }.getOrDefault(emptyList())
    }

    private companion object {
        val FAVORITES_KEY = stringPreferencesKey("favorite_stations")
    }
}
