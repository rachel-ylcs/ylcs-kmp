package love.yinlin.compose.ui.platform

import love.yinlin.service.PlatformContext

expect abstract class HeadlessWebView(context: PlatformContext) {
    fun load(url: String)
    fun destroy()
    abstract fun onUrlIntercepted(url: String): Boolean
    abstract fun onRequestIntercepted(url: String, response: String): Boolean
}