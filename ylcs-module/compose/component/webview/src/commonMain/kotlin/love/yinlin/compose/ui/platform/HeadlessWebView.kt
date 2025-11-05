package love.yinlin.compose.ui.platform

import androidx.compose.runtime.Stable
import love.yinlin.Context

@Stable
expect abstract class HeadlessWebView(context: Context) {
    fun load(url: String)
    fun destroy()
    abstract fun onUrlIntercepted(url: String): Boolean
    abstract fun onRequestIntercepted(url: String, response: String): Boolean
}