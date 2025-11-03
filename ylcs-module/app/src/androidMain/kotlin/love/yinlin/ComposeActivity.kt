package love.yinlin

import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import love.yinlin.extension.catching

abstract class ComposeActivity : ComponentActivity() {
    private val instance get() = (application as ComposeApplication).instance

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        instance.context.bindActivity(this)

        instance.initialize(delay = true)

        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        intent?.let {
            catching { instance.onIntent(it) }
        }

        setContent {
            instance.Layout {
                instance.BeginContent(this)
                instance.Content()
            }
        }
    }

    final override fun onDestroy() {
        instance.destroy(delay = true)

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