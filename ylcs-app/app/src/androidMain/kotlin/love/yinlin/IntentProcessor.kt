package love.yinlin

import android.content.Intent
import android.os.Build
import love.yinlin.common.DeepLinkHandler
import love.yinlin.uri.Scheme
import love.yinlin.uri.Uri
import love.yinlin.uri.toUri
import love.yinlin.extension.catching

object IntentProcessor {
    private object ActionView {
        fun process(data: android.net.Uri) {
            val uri = data.toUri()
            when (uri.scheme) {
                Scheme.Content -> DeepLinkHandler.onOpenUri(uri)
            }
        }
    }

    private object ActionSend {
        object SendText {
            fun process(text: String) {
                when {
                    text.contains("QQ音乐") -> {
                        val result = Uri.parse("https?://\\S+".toRegex().find(text)?.value!!)!!
                        DeepLinkHandler.onOpenUri(result.copy(scheme = Scheme.QQMusic))
                    }
                    text.contains("网易云音乐") -> {
                        val result = Uri.parse("https?://\\S+".toRegex().find(text)?.value!!)!!
                        DeepLinkHandler.onOpenUri(result.copy(scheme = Scheme.NetEaseCloud))
                    }
                }
            }
        }

        object SendBinary {
            fun process(data: android.net.Uri) {
                val uri = data.toUri()
                if (uri.scheme == Scheme.Content) {
                    DeepLinkHandler.onOpenUri(uri)
                }
            }
        }

        fun process(intent: Intent) {
            @Suppress("DEPRECATION")
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
            } else intent.getParcelableExtra(Intent.EXTRA_STREAM)
            if (uri != null) SendBinary.process(uri)
            else SendText.process(intent.getStringExtra(Intent.EXTRA_TEXT) ?: "")
        }
    }

    fun process(intent: Intent) = catching {
        when (intent.action) {
            Intent.ACTION_VIEW -> ActionView.process(intent.data!!)
            Intent.ACTION_SEND -> ActionSend.process(intent)
        }
    }
}