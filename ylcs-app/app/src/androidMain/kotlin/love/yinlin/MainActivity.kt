package love.yinlin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import love.yinlin.compose.*
import love.yinlin.fixup.FixupAndroidStatusBarColor
import love.yinlin.platform.ActualFloatingLyrics

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        service.context.bindActivity(this)
        service.picker.bindActivity(this)

        ActualFloatingLyrics(this).also {
            service.musicFactory.instance.floatingLyrics = it
            if (service.config.enabledFloatingLyrics) it.attach()
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
        (service.musicFactory.instance.floatingLyrics as? ActualFloatingLyrics)?.detach()
        service.musicFactory.instance.floatingLyrics = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        IntentProcessor.process(intent)
    }
}