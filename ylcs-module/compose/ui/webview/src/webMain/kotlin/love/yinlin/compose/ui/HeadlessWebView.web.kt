package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import love.yinlin.foundation.Context

@Stable
actual abstract class HeadlessWebView actual constructor(context: Context) {
    actual fun load(url: String) {}

    actual fun destroy() {}

    actual abstract fun onUrlIntercepted(url: String): Boolean

    actual abstract fun onRequestIntercepted(url: String, response: String): Boolean
}