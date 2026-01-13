package love.yinlin.platform

import love.yinlin.uri.Uri
import love.yinlin.uri.toJvmUri
import java.awt.Desktop

internal data object OSUtil {
    fun openUri(uri: Uri) {
        val desktop = Desktop.getDesktop()
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
            desktop.browse(uri.toJvmUri())
        }
    }
}