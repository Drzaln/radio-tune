package com.rizal.radiotune.data.repository

import com.rizal.radiotune.data.model.Country
import com.rizal.radiotune.data.model.Station
import com.rizal.radiotune.data.model.StationSort
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

/**
 * One page of stations. [rawSize] is how many rows the API returned, which is what
 * offset-based pagination must advance by; [stations] is the filtered, de-duplicated
 * list actually shown.
 */
data class StationPage(
    val stations: List<Station>,
    val rawSize: Int,
)

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

    private val stationCache = ConcurrentHashMap<String, TimedCache<StationPage>>()

    suspend fun getCountries(forceRefresh: Boolean = false): List<Country> = withContext(Dispatchers.IO) {
        countriesCache
            ?.takeIf { !forceRefresh && it.isFresh(COUNTRIES_TTL_MS) }
            ?.let { return@withContext it.value }

        val countries = api.getCountries()
            .map { it.toCountry() }
            .filter { it.code.isNotBlank() && it.stationCount > 0 }
            .distinctBy { it.code }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

        countriesCache = TimedCache(countries)
        countries
    }

    suspend fun getStations(
        countryCode: String,
        query: String = "",
        tags: List<String> = emptyList(),
        sort: StationSort = StationSort.POPULARITY,
        offset: Int = 0,
        limit: Int = PAGE_SIZE,
    ): StationPage = withContext(Dispatchers.IO) {
        val tagParam = tags.filter { it.isNotBlank() }.joinToString(",")
        val key = "$countryCode|${query.trim().lowercase()}|${tagParam.lowercase()}|${sort.name}"
        if (offset == 0) {
            stationCache[key]
                ?.takeIf { it.isFresh(STATIONS_TTL_MS) }
                ?.let { return@withContext it.value }
        }

        val raw = api.searchStations(
            countryCode = countryCode.ifBlank { null },
            name = query.trim().ifBlank { null },
            tag = tagParam.ifBlank { null },
            tagExact = tagParam.isNotBlank(),
            order = sort.apiOrder,
            reverse = sort.reverse,
            limit = limit,
            offset = offset,
        )

        val page = StationPage(
            stations = raw
                .map { it.toStation() }
                .filter { it.playbackUrl.isNotBlank() }
                .dedupe(),
            // Pagination must advance by the rows the API actually returned: the
            // dropped blanks and duplicates still occupy offsets.
            rawSize = raw.size,
        )

        if (offset == 0) {
            if (stationCache.size > MAX_CACHED_QUERIES) stationCache.clear()
            stationCache[key] = TimedCache(page)
        }
        page
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

    /**
     * Radio Browser lists the same station several times — a `.pls` entry next to
     * its direct stream, or two UUIDs pointing at one host. Drops later copies
     * within the page, keeping the first (the API orders by popularity). Applied
     * per page so offset pagination is untouched.
     */
    private fun List<Station>.dedupe(): List<Station> {
        val ids = HashSet<String>()
        val urls = HashSet<String>()
        val names = HashSet<String>()
        return filter { station ->
            val urlKey = station.normalizedStreamUrl()
            val nameKey = "${station.name.trim().lowercase()}|${station.countryCode.lowercase()}"
            ids.add(station.id) && urls.add(urlKey) && names.add(nameKey)
        }
    }

    private fun Station.normalizedStreamUrl(): String = playbackUrl
        .substringBefore('?')
        .substringBefore('#')
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .trimEnd('/')
        .lowercase()

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
     * A fresh random station in [countryCode].
     *
     * `order=random` is a one-shot query, so it must not be served from the HTTP
     * cache: the list endpoints are marked cacheable for 10 minutes, and without a
     * cache buster tuning would keep returning the same station.
     */
    suspend fun randomStation(countryCode: String): Station? = withContext(Dispatchers.IO) {
        api.searchStations(
            countryCode = countryCode.ifBlank { null },
            order = "random",
            limit = 1,
            cacheBuster = System.nanoTime(),
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
