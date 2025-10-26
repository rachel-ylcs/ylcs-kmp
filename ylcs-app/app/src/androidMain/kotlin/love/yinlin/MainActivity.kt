package love.yinlin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import love.yinlin.compose.*
import love.yinlin.fixup.FixupAndroidStatusBarColor
import love.yinlin.platform.ActualFloatingLyrics
import love.yinlin.platform.appNative

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        service.context.bindActivity(this, activityResultRegistry)
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
            AppEntry {
                FixupAndroidStatusBarColor.AutoTheme(window, LocalDarkMode.current)
                ScreenEntry()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (appNative.musicFactory.floatingLyrics as? ActualFloatingLyrics)?.detach()
        appNative.musicFactory.floatingLyrics = null
        appNative.activity = null
        appNative.activityResultRegistry = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        IntentProcessor.process(intent)
    }
}