package com.rizal.radiotune

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rizal.radiotune.di.AppContainer
import com.rizal.radiotune.ui.AppRoot
import com.rizal.radiotune.ui.theme.RadioTuneTheme

class MainActivity : ComponentActivity() {

    private val container: AppContainer
        get() = (application as RadioTuneApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RadioTuneTheme {
                AppRoot()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        container.playerController.connect()
    }

    override fun onStop() {
        super.onStop()
        container.playerController.release()
    }
}
