package com.rizal.radiotune.data.repository

import com.rizal.radiotune.data.model.Country
import com.rizal.radiotune.data.model.Station
import com.rizal.radiotune.data.remote.RadioBrowserApi
import com.rizal.radiotune.data.remote.RemoteConfig
import com.rizal.radiotune.data.remote.dto.toCountry
import com.rizal.radiotune.data.remote.dto.toStation
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class RadioRepository(
    private val api: RadioBrowserApi,
    baseClient: OkHttpClient,
) {

    private val networkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val playlistClient = baseClient.newBuilder()
        .cache(null)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var countriesCache: TimedCache<List<Country>>? = null

    private val stationCache = ConcurrentHashMap<String, TimedCache<List<Station>>>()

    suspend fun getCountries(forceRefresh: Boolean = false): List<Country> = withContext(Dispatchers.IO) {
        countriesCache
            ?.takeIf { !forceRefresh && it.isFresh(COUNTRIES_TTL_MS) }
            ?.let { return@withContext it.value }

        val countries = api.getCountries()
            .map { it.toCountry() }
            .filter { it.code.isNotBlank() && it.stationCount > 0 }
            .sortedWith(compareByDescending<Country> { it.stationCount }.thenBy { it.name })

        countriesCache = TimedCache(countries)
        countries
    }

    suspend fun getStations(
        countryCode: String,
        query: String = "",
        offset: Int = 0,
        limit: Int = PAGE_SIZE,
    ): List<Station> = withContext(Dispatchers.IO) {
        val key = "$countryCode|${query.trim().lowercase()}"
        if (offset == 0) {
            stationCache[key]
                ?.takeIf { it.isFresh(STATIONS_TTL_MS) }
                ?.let { return@withContext it.value }
        }

        val stations = api.searchStations(
            countryCode = countryCode.ifBlank { null },
            name = query.trim().ifBlank { null },
            limit = limit,
            offset = offset,
        )
            .map { it.toStation() }
            .filter { it.playbackUrl.isNotBlank() }

        if (offset == 0) {
            if (stationCache.size > MAX_CACHED_QUERIES) stationCache.clear()
            stationCache[key] = TimedCache(stations)
        }
        stations
    }

    /**
     * Radio Browser's `url_resolved` is usually a direct stream, but some entries
     * point at a `.pls`/`.m3u` playlist that ExoPlayer cannot open. Those are
     * fetched once and replaced with their first stream URL.
     */
    suspend fun resolvePlayableUrl(station: Station): String = withContext(Dispatchers.IO) {
        val url = station.playbackUrl
        if (url.isBlank() || !looksLikePlaylist(url)) return@withContext url
        runCatching { firstStreamFromPlaylist(url) }.getOrNull() ?: url
    }

    private fun looksLikePlaylist(url: String): Boolean {
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        return path.endsWith(".pls") || path.endsWith(".m3u")
    }

    private fun firstStreamFromPlaylist(playlistUrl: String): String? {
        val request = Request.Builder()
            .url(playlistUrl)
            .header("User-Agent", RemoteConfig.USER_AGENT)
            .build()

        playlistClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            return parseFirstStreamUrl(body)
        }
    }

    private fun parseFirstStreamUrl(body: String): String? {
        val lines = body.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }
        val fromPls = lines
            .firstOrNull { it.startsWith("File", ignoreCase = true) && it.contains('=') }
            ?.substringAfter('=')
            ?.trim()
        val candidate = fromPls
            ?: lines.firstOrNull { !it.startsWith("#") && it.startsWith("http", ignoreCase = true) }
        return candidate?.takeIf { it.startsWith("http", ignoreCase = true) }
    }

    /**
     * A fresh random station in [countryCode]. Deliberately bypasses the list
     * cache, otherwise "tuning" would keep returning the same station.
     */
    suspend fun randomStation(countryCode: String): Station? = withContext(Dispatchers.IO) {
        api.searchStations(
            countryCode = countryCode.ifBlank { null },
            order = "random",
            limit = 1,
        ).firstOrNull()
            ?.toStation()
            ?.takeIf { it.playbackUrl.isNotBlank() }
    }

    /** Fire-and-forget click registration; feeds Radio Browser's popularity ranking. */
    fun registerClick(stationUuid: String) {
        if (stationUuid.isBlank()) return
        networkScope.launch {
            runCatching { api.registerClick(stationUuid) }
        }
    }

    private class TimedCache<T>(val value: T) {
        private val storedAt = System.currentTimeMillis()
        fun isFresh(ttlMs: Long) = System.currentTimeMillis() - storedAt < ttlMs
    }

    companion object {
        const val PAGE_SIZE = 80
        private const val COUNTRIES_TTL_MS = 12 * 60 * 60 * 1000L
        private const val STATIONS_TTL_MS = 5 * 60 * 1000L
        private const val MAX_CACHED_QUERIES = 40
    }
}
