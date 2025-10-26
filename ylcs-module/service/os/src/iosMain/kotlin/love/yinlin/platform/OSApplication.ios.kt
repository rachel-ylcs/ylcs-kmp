package love.yinlin.platform

import love.yinlin.uri.Uri
import love.yinlin.extension.catchingDefault
import love.yinlin.service.PlatformContext
import platform.UIKit.UIPasteboard

actual fun buildOSApplication(context: PlatformContext) = object : OSApplication() {
    override suspend fun startAppIntent(uri: Uri): Boolean = catchingDefault(false) { OSUtil.openUri(uri) }

    override fun copyText(text: String): Boolean {
        UIPasteboard.generalPasteboard.setString(text)
        return true
    }
}