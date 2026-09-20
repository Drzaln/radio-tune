package com.rizal.radiotune.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rizal.radiotune.R
import com.rizal.radiotune.ui.components.MiniPlayer
import com.rizal.radiotune.ui.countries.CountriesScreen
import com.rizal.radiotune.ui.favorites.FavoritesScreen
import com.rizal.radiotune.ui.player.PlayerScreen
import com.rizal.radiotune.ui.player.PlayerViewModel
import com.rizal.radiotune.ui.stations.StationsScreen
import com.rizal.radiotune.ui.update.UpdateDialog
import com.rizal.radiotune.ui.update.UpdateViewModel

private object Routes {
    const val COUNTRIES = "countries"
    const val FAVORITES = "favorites"
    const val PLAYER = "player"
    const val STATIONS = "stations/{code}?name={name}"

    fun stations(code: String, name: String): String =
        "stations/${Uri.encode(code)}?name=${Uri.encode(name)}"
}

@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val playerState by playerViewModel.playerState.collectAsStateWithLifecycle()
    val favorites by playerViewModel.favorites.collectAsStateWithLifecycle()
    val favoriteIds = remember(favorites) { favorites.map { it.id }.toSet() }
    val playerStyle by playerViewModel.playerStyle.collectAsStateWithLifecycle()

    val updateViewModel: UpdateViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val updateState by updateViewModel.state.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(playerState.errorMessage) {
        playerState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            playerViewModel.dismissError()
        }
    }

    LaunchedEffect(Unit) {
        updateViewModel.checkOnLaunch()
    }

    LaunchedEffect(updateState.message, updateState.error) {
        val notice = updateState.message ?: updateState.error
        if (notice != null) {
            snackbarHostState.showSnackbar(notice)
            updateViewModel.consumeMessage()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute != Routes.PLAYER

    RequestNotificationPermission()

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                Column {
                    MiniPlayer(
                        state = playerState,
                        onTogglePlayPause = playerViewModel::togglePlayPause,
                        onOpen = { navController.openPlayer() },
                        onStop = playerViewModel::stop,
                    )
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentRoute == Routes.COUNTRIES,
                            onClick = { navController.switchTab(Routes.COUNTRIES) },
                            icon = {
                                Icon(painterResource(R.drawable.ic_radio), contentDescription = null)
                            },
                            label = { Text("Browse") },
                        )
                        NavigationBarItem(
                            selected = currentRoute == Routes.FAVORITES,
                            onClick = { navController.switchTab(Routes.FAVORITES) },
                            icon = {
                                Icon(painterResource(R.drawable.ic_favorite), contentDescription = null)
                            },
                            label = { Text("Favorites") },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.COUNTRIES,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                when (targetState.destination.route) {
                    Routes.PLAYER ->
                        slideIntoContainer(SlideDirection.Up, tween(NAV_DURATION_MS)) +
                            fadeIn(tween(NAV_FADE_MS))

                    Routes.COUNTRIES, Routes.FAVORITES ->
                        fadeIn(tween(NAV_FADE_MS))

                    else ->
                        slideIntoContainer(SlideDirection.Left, tween(NAV_DURATION_MS)) +
                            fadeIn(tween(NAV_FADE_MS))
                }
            },
            exitTransition = {
                fadeOut(tween(NAV_FADE_MS))
            },
            popEnterTransition = {
                fadeIn(tween(NAV_FADE_MS))
            },
            popExitTransition = {
                if (initialState.destination.route == Routes.PLAYER) {
                    slideOutOfContainer(SlideDirection.Down, tween(NAV_DURATION_MS)) +
                        fadeOut(tween(NAV_FADE_MS))
                } else {
                    slideOutOfContainer(SlideDirection.Right, tween(NAV_DURATION_MS)) +
                        fadeOut(tween(NAV_FADE_MS))
                }
            },
        ) {
            composable(Routes.COUNTRIES) {
                CountriesScreen(
                    onCountryClick = { country ->
                        navController.navigate(Routes.stations(country.code, country.name))
                    },
                    onCheckForUpdates = updateViewModel::checkNow,
                )
            }

            composable(Routes.FAVORITES) {
                FavoritesScreen(
                    playerState = playerState,
                    onPlay = playerViewModel::play,
                    onTogglePlayPause = playerViewModel::togglePlayPause,
                    onToggleFavorite = playerViewModel::toggleFavorite,
                )
            }

            composable(Routes.PLAYER) {
                PlayerScreen(
                    state = playerState,
                    favoriteIds = favoriteIds,
                    playerStyle = playerStyle,
                    onSelectStyle = playerViewModel::setPlayerStyle,
                    onBack = { navController.popBackStack() },
                    onTogglePlayPause = playerViewModel::togglePlayPause,
                    onStop = playerViewModel::stop,
                    onToggleFavorite = playerViewModel::toggleFavorite,
                    onSetSleepTimer = playerViewModel::setSleepTimer,
                )
            }

            composable(
                route = Routes.STATIONS,
                arguments = listOf(
                    navArgument("code") { type = NavType.StringType },
                    navArgument("name") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) {
                StationsScreen(
                    playerState = playerState,
                    favoriteIds = favoriteIds,
                    onBack = { navController.popBackStack() },
                    onPlay = playerViewModel::play,
                    onTogglePlayPause = playerViewModel::togglePlayPause,
                    onToggleFavorite = playerViewModel::toggleFavorite,
                )
            }
        }
    }

    UpdateDialog(
        state = updateState,
        onInstall = updateViewModel::install,
        onDismiss = updateViewModel::dismiss,
    )
}

private const val NAV_DURATION_MS = 320
private const val NAV_FADE_MS = 200

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.openPlayer() {
    navigate(Routes.PLAYER) {
        launchSingleTop = true
    }
}

@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Playback still works without the notification permission. */ }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
