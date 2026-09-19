package com.rizal.radiotune.data.remote

import android.content.Context
import com.rizal.radiotune.BuildConfig
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object RemoteConfig {
    /**
     * Radio Browser maintains a round-robin hostname that resolves to the active
     * community mirrors. Change it here to point the whole app at another server.
     */
    const val BASE_URL = "https://all.api.radio-browser.info/"
    const val USER_AGENT = "RadioTune/${BuildConfig.VERSION_NAME} (Android)"
}

object NetworkFactory {

    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    fun buildOkHttpClient(context: Context): OkHttpClient {
        val cache = Cache(File(context.cacheDir, "radio_http_cache"), CACHE_SIZE_BYTES)
        return OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(40, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(HeaderInterceptor)
            .addNetworkInterceptor(CacheControlInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        },
                    )
                }
            }
            .build()
    }

    fun createApi(client: OkHttpClient, json: Json = this.json): RadioBrowserApi =
        Retrofit.Builder()
            .baseUrl(RemoteConfig.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RadioBrowserApi::class.java)

    private const val CACHE_SIZE_BYTES = 8L * 1024 * 1024

    private val HeaderInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", RemoteConfig.USER_AGENT)
            .header("Accept", "application/json")
            .build()
        chain.proceed(request)
    }

    /**
     * Radio Browser does not send cache headers, so list endpoints are made
     * cacheable explicitly. This keeps repeat navigation fast and lets the app
     * fall back to the last response when briefly offline.
     */
    private val CacheControlInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)
        val path = request.url.encodedPath
        val cacheable = request.method == "GET" &&
            (path.startsWith("/json/countries") || path.startsWith("/json/stations"))
        if (cacheable) {
            response.newBuilder()
                .header("Cache-Control", "public, max-age=600")
                .removeHeader("Pragma")
                .build()
        } else {
            response
        }
    }
}
