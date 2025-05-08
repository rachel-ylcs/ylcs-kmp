package love.yinlin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
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
            AppWrapper { App() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (appNative.musicFactory.floatingLyrics as? ActualFloatingLyrics)?.detach()
        appNative.musicFactory.floatingLyrics = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        IntentProcessor.process(intent, ViewModelProvider(this))
    }
}