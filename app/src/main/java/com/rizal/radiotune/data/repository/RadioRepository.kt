package com.rizal.radiotune.data.repository

import com.rizal.radiotune.data.model.Country
import com.rizal.radiotune.data.model.Station
import com.rizal.radiotune.data.remote.RadioBrowserApi
import com.rizal.radiotune.data.remote.dto.toCountry
import com.rizal.radiotune.data.remote.dto.toStation
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RadioRepository(
    private val api: RadioBrowserApi,
) {

    private val networkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
