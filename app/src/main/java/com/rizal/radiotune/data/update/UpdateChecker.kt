package com.rizal.radiotune.data.update

import com.rizal.radiotune.data.remote.RemoteConfig
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class UpdateChecker(
    baseClient: OkHttpClient,
) {

    private val client = baseClient.newBuilder().followRedirects(false).build()

    /**
     * Resolves the newest published release by following GitHub's
     * `/releases/latest` redirect to `/releases/tag/<tag>`.
     */
    suspend fun latestRelease(): UpdateInfo? = withContext(Dispatchers.IO) {
        val tag = resolveLatestTag() ?: return@withContext null
        val versionName = tag.removePrefix("v").trim()
        if (versionName.isBlank()) return@withContext null

        UpdateInfo(
            tag = tag,
            versionName = versionName,
            versionCode = versionCodeOf(versionName),
            releasePageUrl = UpdateConfig.releasePageUrl(tag),
            downloadUrls = listOf(
                UpdateConfig.latestDownloadUrl(UpdateConfig.STABLE_ASSET_NAME),
                UpdateConfig.downloadUrl(tag, UpdateConfig.versionedAssetName(versionName)),
            ),
        )
    }

    private fun resolveLatestTag(): String? {
        val request = Request.Builder()
            .url(UpdateConfig.LATEST_RELEASE_URL)
            .header("User-Agent", RemoteConfig.USER_AGENT)
            .header("Cache-Control", "no-cache")
            .build()

        client.newCall(request).execute().use { response ->
            val location = response.header("Location")
            if (!location.isNullOrBlank()) {
                return location.substringAfterLast("/releases/tag/", "")
                    .trim()
                    .takeIf { it.isNotEmpty() }
            }
            if (!response.isSuccessful) {
                throw IOException("GitHub responded with HTTP ${response.code}")
            }
        }
        return null
    }

    companion object {
        /** Mirrors the versionCode scheme used by the release workflow. */
        fun versionCodeOf(versionName: String): Int {
            val parts = versionName.split('.', '-', '_')
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            return major * 10_000 + minor * 100 + patch
        }
    }
}
