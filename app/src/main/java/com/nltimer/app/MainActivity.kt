package com.nltimer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nltimer.core.data.SettingsPrefs
import com.nltimer.core.designsystem.component.AppIntroScreen
import com.nltimer.core.designsystem.theme.NLtimerTheme
import com.nltimer.core.designsystem.theme.Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsPrefs: SettingsPrefs

    private val themeReady = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !themeReady.get() }
        enableEdgeToEdge()

        setContent {
            val theme by settingsPrefs.getThemeFlow()
                .collectAsStateWithLifecycle(initialValue = null)

            LaunchedEffect(theme) {
                if (theme != null) themeReady.set(true)
            }

            theme?.let { currentTheme ->
                val hasSeenIntro by settingsPrefs.getHasSeenIntroFlow()
                    .collectAsStateWithLifecycle(initialValue = true)
                val scope = rememberCoroutineScope()

                NLtimerTheme(theme = currentTheme) {
                    if (!hasSeenIntro) {
                        AppIntroScreen(
                            onFinish = { scope.launch { settingsPrefs.setHasSeenIntro(true) } },
                        )
                    } else {
                        NLtimerApp(settingsPrefs = settingsPrefs)
                    }
                }
            }
        }
    }
}
