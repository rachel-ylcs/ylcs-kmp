package love.yinlin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import love.yinlin.ui.screen.music.ScreenImportMusic

class DeepLink(private val model: AppModel) {
    private fun actionView(uri: Uri) {
        val args = HashMap<String, String>()
        for (name in uri.queryParameterNames) uri.getQueryParameter(name)?.let { args[name] = it }
        when (uri.scheme) {
            "content" -> {
                model.navigate(ScreenImportMusic.Args(uri.toString()))
            }
            "rachel" -> {

            }
        }
    }

    private fun actionSend(type: String, bundle: Bundle) {
        when (type) {
            "text/plain" -> {

            }
        }
    }

    fun process(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                if (uri != null) actionView(uri)
            }
            Intent.ACTION_SEND -> {
                val bundle = intent.extras
                val type = intent.type
                if (bundle != null && type != null) actionSend(type, bundle)
            }
            else -> {}
        }
    }
}