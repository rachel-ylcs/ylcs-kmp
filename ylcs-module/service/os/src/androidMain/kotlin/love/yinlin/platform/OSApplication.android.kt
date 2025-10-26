package love.yinlin.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import love.yinlin.uri.Uri
import love.yinlin.extension.catchingDefault
import love.yinlin.service.PlatformContext

actual fun buildOSApplication(context: PlatformContext) = object : OSApplication() {
    override suspend fun startAppIntent(uri: Uri): Boolean = catchingDefault(false) {
        OSUtil.openUri(context, uri)
        true
    }

    override fun copyText(text: String): Boolean = catchingDefault(false) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("", text)
        clipboard.setPrimaryClip(clip)
        true
    }
}