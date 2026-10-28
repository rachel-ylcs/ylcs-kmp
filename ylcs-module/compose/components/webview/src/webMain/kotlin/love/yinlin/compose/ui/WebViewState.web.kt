package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.BitmapPainter
import love.yinlin.extension.createElement
import org.w3c.dom.HTMLIFrameElement

@Stable
actual class WebViewState actual constructor(initUrl: String) : PlatformViewState<HTMLIFrameElement, WebViewParameters>() {
    private var requestedUrl = initUrl
    actual var url: String
        get() = requestedUrl
        set(value) {
            requestedUrl = value
            withView { it.src = value }
        }
    actual val loadingState: WebViewLoadingState = WebViewLoadingState.Finished
    actual val title: String = ""
    actual val icon: BitmapPainter? = null
    actual val canGoBack: Boolean = false
    actual val canGoForward: Boolean = false
    actual val error: WebViewError? = null
    actual fun goBack() {}
    actual fun goForward() {}
    actual fun evaluateJavaScript(script: String) {}

    override fun factory(environment: PlatformViewEnvironment): HTMLIFrameElement = createElement {
        frameBorder = "0"
        referrerPolicy = "no-referrer"
    }

    override fun synchronize(view: HTMLIFrameElement, previous: WebViewParameters?, next: WebViewParameters) {
        if (previous == null) view.src = requestedUrl
    }

    override fun detach(view: HTMLIFrameElement) {
        view.src = "about:blank"
    }
}
