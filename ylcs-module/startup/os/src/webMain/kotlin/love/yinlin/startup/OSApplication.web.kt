package love.yinlin.startup

import kotlinx.browser.window
import love.yinlin.foundation.Context
import love.yinlin.uri.Uri
import kotlin.js.ExperimentalWasmJsInterop

actual fun buildOSApplication(context: Context) = object : OSApplication() {
    override suspend fun startAppIntent(uri: Uri): Boolean {
        OSUtil.openUri(uri)
        return true
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    override fun copyText(text: String): Boolean {
        window.navigator.clipboard.writeText(text)
        return true
    }
}