package com.rizal.radiotune.di

import android.content.Context
import com.rizal.radiotune.data.local.dataStore
import com.rizal.radiotune.data.remote.NetworkFactory
import com.rizal.radiotune.data.repository.FavoritesRepository
import com.rizal.radiotune.data.repository.RadioRepository
import com.rizal.radiotune.data.update.ApkDownloader
import com.rizal.radiotune.data.update.UpdateChecker
import com.rizal.radiotune.data.update.UpdateRepository
import com.rizal.radiotune.playback.PlayerController
import kotlinx.serialization.json.Json

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val json: Json = NetworkFactory.json

    private val okHttpClient by lazy { NetworkFactory.buildOkHttpClient(appContext) }

    private val radioBrowserApi by lazy { NetworkFactory.createApi(okHttpClient, json) }

    val radioRepository: RadioRepository by lazy { RadioRepository(radioBrowserApi, okHttpClient) }

    val favoritesRepository: FavoritesRepository by lazy {
        FavoritesRepository(appContext.dataStore, json)
    }

    val playerController: PlayerController by lazy { PlayerController(appContext, json) }

    val updateRepository: UpdateRepository by lazy {
        UpdateRepository(
            context = appContext,
            checker = UpdateChecker(okHttpClient),
            downloader = ApkDownloader(okHttpClient, appContext),
            dataStore = appContext.dataStore,
        )
    }
}
