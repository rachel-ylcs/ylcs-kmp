package love.yinlin.platform

import love.yinlin.uri.Uri
import love.yinlin.service.PlatformContext
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

actual fun buildOSApplication(context: PlatformContext) = object : OSApplication() {
    override suspend fun startAppIntent(uri: Uri): Boolean {
        OSUtil.openUri(uri)
        return true
    }

    override fun copyText(text: String): Boolean {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, null)
        return true
    }
}