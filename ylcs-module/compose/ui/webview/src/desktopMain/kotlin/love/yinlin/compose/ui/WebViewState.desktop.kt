package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.BitmapPainter

@Stable
actual class WebViewState actual constructor(initUrl: String) {
    actual var url: String = initUrl
    actual val loadingState: WebViewLoadingState = WebViewLoadingState.Initializing
    actual val title: String = ""
    actual val icon: BitmapPainter? = null
    actual val canGoBack: Boolean = false
    actual val canGoForward: Boolean = false
    actual val error: WebViewError? = null
    actual fun goBack() {}
    actual fun goForward() {}
    actual fun evaluateJavaScript(script: String) {}
}