package love.yinlin.ui.component.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import kotlinx.browser.document
import love.yinlin.extension.mutableRefStateOf
import love.yinlin.ui.CustomUI
import org.w3c.dom.HTMLIFrameElement

@Stable
actual class WebPageState actual constructor(val settings: WebPageSettings, initUrl: String) {
    internal val webview = mutableRefStateOf<HTMLIFrameElement?>(null)
    actual var url: String = initUrl
	actual val loadingState: WebPageLoadingState = WebPageLoadingState.Finished
	actual val title: String = ""
	actual val icon: BitmapPainter? = null
	actual val canGoBack: Boolean = false
	actual val canGoForward: Boolean = false
	actual val error: WebPageError? = null
	actual fun goBack() {}
	actual fun goForward() {}
	actual fun evaluateJavaScript(script: String) {}
}

@Composable
actual fun WebPage(
	state: WebPageState,
	modifier: Modifier
) {
    CustomUI(
        view = state.webview,
        factory = {
            (document.createElement("iframe") as HTMLIFrameElement).also { iframe ->
                iframe.frameBorder = "0"
                iframe.referrerPolicy = "no-referrer"
                iframe.src = state.url
            }
        },
        modifier = modifier
    )
}

@Stable
actual abstract class HeadlessBrowser actual constructor() {
	actual fun load(url: String) {}

	actual fun destroy() {}

	actual abstract fun onUrlIntercepted(url: String): Boolean

	actual abstract fun onRequestIntercepted(url: String, response: String): Boolean
}