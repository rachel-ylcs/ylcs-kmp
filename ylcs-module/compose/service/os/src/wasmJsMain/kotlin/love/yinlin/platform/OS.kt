package love.yinlin.platform

import androidx.compose.runtime.Stable
import kotlinx.browser.window
import love.yinlin.uri.Uri

@Stable
internal data object OSUtil {
    fun openUri(uri: Uri) {
        window.open(uri.toString(), "_blank")
    }

    fun osApplicationIsStandalone() = window.matchMedia("(display-mode: standalone)").matches
}