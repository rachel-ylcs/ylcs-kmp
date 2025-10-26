package love.yinlin.compose.ui.platform

import love.yinlin.service.PlatformPage

expect abstract class HeadlessWebView(context: PlatformPage) {
    fun load(url: String)
    fun destroy()
    abstract fun onUrlIntercepted(url: String): Boolean
    abstract fun onRequestIntercepted(url: String, response: String): Boolean
}