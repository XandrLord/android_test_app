package com.davidrevolt.blelink.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import com.davidrevolt.blelink.ui.AppState
import com.davidrevolt.feature.control.controlScreen
import com.davidrevolt.feature.control.navigateToControl
import com.davidrevolt.feature.home.HOME_ROUTE
import com.davidrevolt.feature.home.homeScreen

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun AppNavHost(appState: AppState) {
    val navController = appState.navController
    val startDestination = HOME_ROUTE

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        homeScreen(onConnectClick = navController::navigateToControl)
        controlScreen(onBackClick = navController::popBackStack)
    }
}