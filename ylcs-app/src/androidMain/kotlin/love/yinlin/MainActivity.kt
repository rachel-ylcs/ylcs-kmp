package love.yinlin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import love.yinlin.common.Scheme
import love.yinlin.common.toUri
import love.yinlin.platform.appNative

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appNative.activityResultRegistry = activityResultRegistry
        setContent {
            AppWrapper {
                App()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        try {
            val deeplink = ViewModelProvider(this)[AppModel::class.java].deeplink
            when (intent.action) {
                Intent.ACTION_VIEW -> {
                    val uri = intent.data?.toUri()
                    if (uri != null && uri.scheme == Scheme.Content) deeplink.process(uri)
                }
            }
        }
        catch (_: Throwable) { }
    }
}