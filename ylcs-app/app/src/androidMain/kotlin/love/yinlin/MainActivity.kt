package love.yinlin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.view.WindowInsetsControllerCompat
import love.yinlin.compose.*
import love.yinlin.platform.ActualFloatingLyrics
import love.yinlin.platform.app
import love.yinlin.platform.appNative
import love.yinlin.resources.Res
import love.yinlin.resources.xwwk

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        appNative.activity = this
        appNative.activityResultRegistry = activityResultRegistry

        ActualFloatingLyrics(this).also {
            appNative.musicFactory.floatingLyrics = it
            if (appNative.config.enabledFloatingLyrics) it.attach()
        }

        intent?.let {
            IntentProcessor.process(it)
        }

        setContent {
            App(
                deviceFactory = { maxWidth, maxHeight -> Device(maxWidth, maxHeight) },
                themeMode = app.config.themeMode,
                fontScale = app.config.fontScale,
                mainFontResource = Res.font.xwwk,
                modifier = Modifier.fillMaxSize()
            ) { _, _ ->
                val isDarkMode = LocalDarkMode.current
                LaunchedEffect(isDarkMode) {
                    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isDarkMode
                }
                AppUI()
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