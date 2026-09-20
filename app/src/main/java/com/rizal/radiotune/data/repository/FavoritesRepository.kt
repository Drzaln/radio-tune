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
import kotlinx.coroutines.flow.first
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

    /** Persists a user-defined ordering of the saved stations. */
    suspend fun move(from: Int, to: Int) {
        dataStore.edit { preferences ->
            val current = decode(preferences[FAVORITES_KEY]).toMutableList()
            if (from !in current.indices || to !in current.indices || from == to) return@edit
            current.add(to, current.removeAt(from))
            preferences[FAVORITES_KEY] = json.encodeToString(current)
        }
    }

    /** Serializes the saved stations for a backup file. */
    suspend fun exportJson(): String = json.encodeToString(favorites.first())

    /**
     * Merges stations from a backup file, keeping existing entries and their
     * order. Returns how many new stations were added.
     */
    suspend fun import(raw: String): Int {
        val parsed = runCatching { json.decodeFromString<List<Station>>(raw) }
            .getOrElse { return 0 }
            .filter { it.id.isNotBlank() }
            .distinctBy { it.id }
        var added = 0
        dataStore.edit { preferences ->
            val current = decode(preferences[FAVORITES_KEY])
            val incoming = parsed.filter { candidate -> current.none { it.id == candidate.id } }
            added = incoming.size
            preferences[FAVORITES_KEY] = json.encodeToString(current + incoming)
        }
        return added
    }

    private fun decode(raw: String?): List<Station> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<Station>>(raw) }.getOrDefault(emptyList())
    }

    private companion object {
        val FAVORITES_KEY = stringPreferencesKey("favorite_stations")
    }
}
