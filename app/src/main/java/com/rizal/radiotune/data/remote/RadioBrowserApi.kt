package com.rizal.radiotune.data.remote

import com.rizal.radiotune.data.remote.dto.CountryDto
import com.rizal.radiotune.data.remote.dto.StationDto
import com.rizal.radiotune.data.remote.dto.StationUrlDto
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface RadioBrowserApi {

    @GET("json/countries")
    suspend fun getCountries(): List<CountryDto>

    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("countrycode") countryCode: String? = null,
        @Query("name") name: String? = null,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("is_https") isHttps: Boolean? = null,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
    ): List<StationDto>

    @Headers("Accept: application/json")
    @GET("json/url/{uuid}")
    suspend fun registerClick(@Path("uuid") uuid: String): StationUrlDto
}
