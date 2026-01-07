package love.yinlin.platform

import androidx.compose.runtime.Stable
import love.yinlin.Context
import love.yinlin.uri.Uri
import love.yinlin.extension.catchingDefault
import platform.UIKit.UIPasteboard

@Stable
actual fun buildOSApplication(context: Context) = object : OSApplication() {
    override suspend fun startAppIntent(uri: Uri): Boolean = catchingDefault(false) { OSUtil.openUri(uri) }

    override fun copyText(text: String): Boolean {
        UIPasteboard.generalPasteboard.setString(text)
        return true
    }
}