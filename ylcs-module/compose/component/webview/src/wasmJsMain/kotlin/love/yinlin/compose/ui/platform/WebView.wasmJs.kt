package love.yinlin.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.browser.document
import love.yinlin.platform.PlatformView
import org.w3c.dom.HTMLIFrameElement

@Composable
actual fun WebView(
    state: WebViewState,
    modifier: Modifier
) {
    PlatformView(
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