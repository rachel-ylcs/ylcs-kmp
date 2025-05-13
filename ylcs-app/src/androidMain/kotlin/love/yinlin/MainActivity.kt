package love.yinlin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowInsetsControllerCompat
import love.yinlin.common.LocalDarkMode
import love.yinlin.platform.ActualFloatingLyrics
import love.yinlin.platform.appNative

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        appNative.activityResultRegistry = activityResultRegistry

        appNative.musicFactory.floatingLyrics = ActualFloatingLyrics(this).apply { attach() }

        setContent {
            AppWrapper {
                val isDarkMode = LocalDarkMode.current
                LaunchedEffect(isDarkMode) {
                    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isDarkMode
                }
                App()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (appNative.musicFactory.floatingLyrics as? ActualFloatingLyrics)?.detach()
        appNative.musicFactory.floatingLyrics = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        IntentProcessor.process(intent)
    }
}