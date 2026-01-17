package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.BitmapPainter
import kotlinx.browser.document
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

    override fun build(): HTMLIFrameElement {
        val iframe = document.createElement("iframe") as HTMLIFrameElement
        iframe.frameBorder = "0"
        iframe.referrerPolicy = "no-referrer"
        iframe.src = initUrl
        return iframe
    }
}