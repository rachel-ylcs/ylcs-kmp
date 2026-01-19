package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.BitmapPainter
import love.yinlin.extension.createElement
import org.w3c.dom.HTMLIFrameElement

@Stable
actual class WebViewState actual constructor(private val initUrl: String) : PlatformView<HTMLIFrameElement>() {
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

    override fun build(): HTMLIFrameElement = createElement {
        frameBorder = "0"
        referrerPolicy = "no-referrer"
        src = initUrl
    }
}