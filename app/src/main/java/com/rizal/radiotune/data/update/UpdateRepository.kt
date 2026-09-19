package com.rizal.radiotune.data.update

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import java.io.File
import kotlinx.coroutines.flow.first

sealed interface UpdateCheckResult {
    data class Available(val update: UpdateInfo) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult

    /** Automatic check skipped because one ran recently. */
    data object Skipped : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

class UpdateRepository(
    private val context: Context,
    private val checker: UpdateChecker,
    private val downloader: ApkDownloader,
    private val dataStore: DataStore<Preferences>,
) {

    suspend fun checkForUpdate(
        currentVersionCode: Int,
        force: Boolean,
    ): UpdateCheckResult {
        if (!force && !isCheckDue()) return UpdateCheckResult.Skipped

        return try {
            val latest = checker.latestRelease()
            markChecked()
            when {
                latest == null ->
                    UpdateCheckResult.Failed("Could not read the latest release")

                latest.versionCode > currentVersionCode ->
                    UpdateCheckResult.Available(latest)

                else -> UpdateCheckResult.UpToDate
            }
        } catch (error: Exception) {
            UpdateCheckResult.Failed(error.message ?: "Update check failed")
        }
    }

    suspend fun download(update: UpdateInfo, onProgress: (Int) -> Unit): File =
        downloader.download(update.downloadUrls, onProgress)

    fun install(apk: File) = ApkInstaller.install(context, apk)

    private suspend fun isCheckDue(): Boolean {
        val lastCheck = dataStore.data.first()[LAST_CHECK_KEY] ?: 0L
        return System.currentTimeMillis() - lastCheck >= CHECK_INTERVAL_MS
    }

    private suspend fun markChecked() {
        dataStore.edit { preferences ->
            preferences[LAST_CHECK_KEY] = System.currentTimeMillis()
        }
    }

    private companion object {
        val LAST_CHECK_KEY = longPreferencesKey("last_update_check")
        const val CHECK_INTERVAL_MS = 12 * 60 * 60 * 1000L
    }
}
