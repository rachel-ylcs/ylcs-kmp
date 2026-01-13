package love.yinlin.platform

import love.yinlin.Context
import love.yinlin.uri.Uri
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

actual fun buildOSApplication(context: Context) = object : OSApplication() {
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