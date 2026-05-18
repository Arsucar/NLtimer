package com.nltimer.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.nltimer.core.data.SettingsPrefs

@Composable
fun NLtimerApp(settingsPrefs: SettingsPrefs) {
    val navController = rememberNavController()
    NLtimerScaffold(navController = navController, settingsPrefs = settingsPrefs)
}
