package love.yinlin.platform

import kotlinx.browser.window
import love.yinlin.common.uri.Uri

internal data object OSUtil {
    fun openUri(uri: Uri) {
        window.open(uri.toString(), "_blank")
    }

    fun osApplicationIsStandalone() = window.matchMedia("(display-mode: standalone)").matches
}