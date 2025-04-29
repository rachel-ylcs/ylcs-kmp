package love.yinlin

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import love.yinlin.common.Scheme
import love.yinlin.common.toUri
import love.yinlin.data.MimeType
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
                    val data = intent.data
                    val uri = data?.toUri()
                    if (uri != null && uri.scheme == Scheme.Content) {
                        deeplink.process(uri)
                    }
                }
                Intent.ACTION_SEND -> {
                    when (intent.type) {
                        MimeType.TEXT -> {

                        }
                        MimeType.BINARY -> {
                            @Suppress("DEPRECATION")
                            val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                                else intent.getParcelableExtra(Intent.EXTRA_STREAM)
                            val uri = data?.toUri()
                            if (uri != null && uri.scheme == Scheme.Content) {
                                deeplink.process(uri)
                            }
                        }
                    }
                }
            }
        }
        catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}