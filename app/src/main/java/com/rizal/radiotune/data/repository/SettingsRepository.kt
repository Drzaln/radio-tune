package com.rizal.radiotune.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Simple key/value preferences. Stores the player style by name so this layer
 * stays independent of the UI enum.
 */
class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {

    val playerStyleName: Flow<String?> = dataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { preferences -> preferences[PLAYER_STYLE_KEY] }
        .distinctUntilChanged()

    suspend fun setPlayerStyleName(name: String) {
        dataStore.edit { preferences ->
            preferences[PLAYER_STYLE_KEY] = name
        }
    }

    private companion object {
        val PLAYER_STYLE_KEY = stringPreferencesKey("player_style")
    }
}
