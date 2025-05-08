package love.yinlin

import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import love.yinlin.common.DeepLink
import love.yinlin.common.Scheme
import love.yinlin.common.Uri
import love.yinlin.common.toUri
import love.yinlin.data.MimeType

object IntentProcessor {
    private object ActionView {
        fun process(deeplink: DeepLink, data: android.net.Uri) {
            val uri = data.toUri()
            when (uri.scheme) {
                Scheme.Content -> deeplink.process(uri)
            }
        }
    }

    private object ActionSend {
        object SendText {
            fun process(deeplink: DeepLink, text: String) {
                when {
                    text.contains("QQ音乐") -> {
                        val result = Uri.parse("https?://\\S+".toRegex().find(text)?.value!!)!!
                        deeplink.process(result.copy(scheme = Scheme.QQMusic))
                    }
                    text.contains("网易云音乐") -> {
                        val result = Uri.parse("https?://\\S+".toRegex().find(text)?.value!!)!!
                        deeplink.process(result.copy(scheme = Scheme.NetEaseCloud))
                    }
                }
            }
        }

        object SendBinary {
            fun process(deeplink: DeepLink, data: android.net.Uri) {
                val uri = data.toUri()
                if (uri.scheme == Scheme.Content) {
                    deeplink.process(uri)
                }
            }
        }

        fun process(deeplink: DeepLink, intent: Intent) {
            @Suppress("DEPRECATION")
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
            } else intent.getParcelableExtra(Intent.EXTRA_STREAM)
            if (uri != null) SendBinary.process(deeplink, uri)
            else SendText.process(deeplink, intent.getStringExtra(Intent.EXTRA_TEXT) ?: "")
        }
    }

    fun process(intent: Intent, viewModelProvider: ViewModelProvider) {
        try {
            val deeplink = viewModelProvider[AppModel::class.java].deeplink
            when (intent.action) {
                Intent.ACTION_VIEW -> ActionView.process(deeplink, intent.data!!)
                Intent.ACTION_SEND -> ActionSend.process(deeplink, intent)
            }
        }
        catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}