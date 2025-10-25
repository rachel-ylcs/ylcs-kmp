package love.yinlin.platform

import love.yinlin.common.uri.Uri
import love.yinlin.common.uri.toJvmUri
import java.awt.Desktop

internal external fun requestSingleInstance(): Boolean
internal external fun releaseSingleInstance()

internal data object OSUtil {
    fun openUri(uri: Uri) {
        val desktop = Desktop.getDesktop()
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
            desktop.browse(uri.toJvmUri())
        }
    }
}