package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.BitmapPainter
import kotlinx.cinterop.ObjCSignatureOverride
import love.yinlin.compose.extension.mutableRefStateOf
import platform.Foundation.NSError
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.darwin.NSObject

@Stable
actual class WebViewState actual constructor(initUrl: String) : PlatformView<WKWebView>() {
    internal var stateUrl: String by mutableStateOf(initUrl)
    actual var url: String get() = stateUrl
        set(value) { host?.loadRequest(NSMutableURLRequest(NSURL(string = value))) }

    internal var stateLoadingState: WebViewLoadingState by mutableRefStateOf(WebViewLoadingState.Initializing)
    actual val loadingState: WebViewLoadingState by derivedStateOf { stateLoadingState }

    internal var stateTitle: String by mutableStateOf("")
    actual val title: String by derivedStateOf { stateTitle }

    internal var stateIcon: BitmapPainter? by mutableRefStateOf(null)
    actual val icon: BitmapPainter? by derivedStateOf { stateIcon }

    internal var stateCanGoBack: Boolean by mutableStateOf(false)
    actual val canGoBack: Boolean by derivedStateOf { stateCanGoBack }

    internal var stateCanGoForward: Boolean by mutableStateOf(false)
    actual val canGoForward: Boolean by derivedStateOf { stateCanGoForward }

    internal var stateError: WebViewError? by mutableRefStateOf(null)
    actual val error: WebViewError? by derivedStateOf { stateError }

    private val protocol: WKNavigationDelegateProtocol = object : NSObject(), WKNavigationDelegateProtocol {
        @ObjCSignatureOverride
        override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
            webView.URL?.absoluteString?.let { stateUrl = it }
            stateLoadingState = WebViewLoadingState.Loading(0f)
            webView.title?.let { stateTitle = it }
        }

        @ObjCSignatureOverride
        override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
            webView.URL?.absoluteString?.let { stateUrl = it }
            stateLoadingState = WebViewLoadingState.Finished
            webView.title?.let { stateTitle = it }
        }

        @ObjCSignatureOverride
        override fun webView(webView: WKWebView, didCommitNavigation: WKNavigation?) {
            stateLoadingState = WebViewLoadingState.Loading(webView.estimatedProgress.toFloat())
        }

        @ObjCSignatureOverride
        override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) {
            stateError = WebViewError(withError.code, withError.description.toString())
        }
    }

    actual fun goBack() {
        host?.let {
            if (it.canGoBack) it.goBack()
        }
    }

    actual fun goForward() {
        host?.let {
            if (it.canGoForward) it.goForward()
        }
    }

    actual fun evaluateJavaScript(script: String) {
        host?.evaluateJavaScript(script, null)
    }

    override fun build(): WKWebView {
        val webview = WKWebView()
        webview.setUserInteractionEnabled(true)
        webview.allowsBackForwardNavigationGestures = true
        webview.navigationDelegate = protocol
        if (stateUrl.isNotEmpty()) webview.loadRequest(NSMutableURLRequest(NSURL(string = stateUrl)))
        return webview
    }
}