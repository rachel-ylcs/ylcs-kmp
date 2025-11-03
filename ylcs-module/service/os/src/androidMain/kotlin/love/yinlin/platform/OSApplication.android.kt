package love.yinlin.platform

import android.content.ClipData
import android.content.ClipboardManager
import love.yinlin.AndroidContext
import love.yinlin.Context
import love.yinlin.uri.Uri
import love.yinlin.extension.catchingDefault

actual fun buildOSApplication(context: Context) = object : OSApplication() {
    override suspend fun startAppIntent(uri: Uri): Boolean = catchingDefault(false) {
        OSUtil.openUri(context.application, uri)
        true
    }

    override fun copyText(text: String): Boolean = catchingDefault(false) {
        val clipboard = context.application.getSystemService(AndroidContext.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("", text)
        clipboard.setPrimaryClip(clip)
        true
    }
}