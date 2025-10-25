package love.yinlin.platform

import kotlinx.browser.window
import love.yinlin.common.uri.Uri
import love.yinlin.service.PlatformContext

actual fun buildOSApplication(context: PlatformContext) = object : OSApplication() {
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