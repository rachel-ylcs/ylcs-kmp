package love.yinlin.compose.ui.platform

import androidx.compose.runtime.Stable
import love.yinlin.service.PlatformPage

@Stable
actual abstract class HeadlessWebView actual constructor(context: PlatformPage) {
    actual fun load(url: String) {}

    actual fun destroy() {}

    actual abstract fun onUrlIntercepted(url: String): Boolean

    actual abstract fun onRequestIntercepted(url: String, response: String): Boolean
}