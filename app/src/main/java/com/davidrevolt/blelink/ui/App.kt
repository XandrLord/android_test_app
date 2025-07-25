package com.davidrevolt.blelink.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.davidrevolt.blelink.navigation.AppNavHost


@Composable
fun App(appState: AppState = rememberAppState()) {
    Scaffold(modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            AppNavHost(appState = appState)
        }
    }
}