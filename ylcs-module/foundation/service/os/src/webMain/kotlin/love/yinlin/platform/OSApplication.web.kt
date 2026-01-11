package love.yinlin.platform

import androidx.compose.runtime.Stable
import kotlinx.browser.window
import love.yinlin.Context
import love.yinlin.uri.Uri
import kotlin.js.ExperimentalWasmJsInterop

@Stable
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