package com.rizal.radiotune

import android.app.Application
import com.rizal.radiotune.di.AppContainer

class RadioTuneApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
