package love.yinlin.compose.ui.platform

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.BitmapPainter
import love.yinlin.compose.mutableRefStateOf
import org.w3c.dom.HTMLIFrameElement

@Stable
actual class WebViewState actual constructor(val settings: WebViewConfig, initUrl: String) {
    internal val webview = mutableRefStateOf<HTMLIFrameElement?>(null)
    actual var url: String = initUrl
    actual val loadingState: WebViewLoadingState = WebViewLoadingState.Finished
    actual val title: String = ""
    actual val icon: BitmapPainter? = null
    actual val canGoBack: Boolean = false
    actual val canGoForward: Boolean = false
    actual val error: WebViewError? = null
    actual fun goBack() {}
    actual fun goForward() {}
    actual fun evaluateJavaScript(script: String) {}
}