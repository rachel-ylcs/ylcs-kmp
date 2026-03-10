package love.yinlin.compose

import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import love.yinlin.extension.catching

abstract class ComposeActivity : ComponentActivity() {
    private val instance by lazy { (application as ComposeApplication).instance }

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        instance.context.bindActivity(this)

        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        lifecycleScope.launch {
            with(instance) { openServiceLater() }
        }

        intent?.let {
            catching { instance.onIntent(it) }
        }

        setContent {
            instance.ComposedLayout {
                Fixup.StatusBarAutoTheme(window, Theme.darkMode)

                instance.BeginContent(this)
                instance.Content()
            }
        }
    }

    final override fun onDestroy() {
        instance.closeServiceBefore()

        super.onDestroy()
    }

    final override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        catching { instance.onIntent(intent) }
    }

    final override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
    }
}