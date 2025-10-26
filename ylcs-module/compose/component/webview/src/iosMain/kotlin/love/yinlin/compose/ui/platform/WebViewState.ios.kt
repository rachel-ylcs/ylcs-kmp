package love.yinlin.compose.ui.platform

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.BitmapPainter
import kotlinx.cinterop.ObjCSignatureOverride
import love.yinlin.compose.mutableRefStateOf
import platform.Foundation.NSError
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.darwin.NSObject

@Stable
actual class WebViewState actual constructor(val settings: WebViewConfig, initUrl: String) {
    internal val webview = mutableRefStateOf<WKWebView?>(null)

    internal var mUrl: String by mutableStateOf(initUrl)
    actual var url: String get() = mUrl
        set(value) { webview.value?.loadRequest(NSMutableURLRequest(NSURL(string = value))) }

    internal var mLoadingState: WebViewLoadingState by mutableRefStateOf(WebViewLoadingState.Initializing)
    actual val loadingState: WebViewLoadingState get() = mLoadingState

    internal var mTitle: String by mutableStateOf("")
    actual val title: String get() = mTitle

    internal var mIcon: BitmapPainter? by mutableRefStateOf(null)
    actual val icon: BitmapPainter? get() = mIcon

    internal var mCanGoBack: Boolean by mutableStateOf(false)
    actual val canGoBack: Boolean get() = mCanGoBack

    internal var mCanGoForward: Boolean by mutableStateOf(false)
    actual val canGoForward: Boolean get() = mCanGoForward

    internal var mError: WebViewError? by mutableRefStateOf(null)
    actual val error: WebViewError? get() = mError

    internal val protocol: WKNavigationDelegateProtocol = object : NSObject(), WKNavigationDelegateProtocol {
        @ObjCSignatureOverride
        override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
            webView.URL?.absoluteString?.let { mUrl = it }
            mLoadingState = WebViewLoadingState.Loading(0f)
            webView.title?.let { mTitle = it }
        }

        @ObjCSignatureOverride
        override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
            webView.URL?.absoluteString?.let { mUrl = it }
            mLoadingState = WebViewLoadingState.Finished
            webView.title?.let { mTitle = it }
        }

        @ObjCSignatureOverride
        override fun webView(webView: WKWebView, didCommitNavigation: WKNavigation?) {
            mLoadingState = WebViewLoadingState.Loading(webView.estimatedProgress.toFloat())
        }

        @ObjCSignatureOverride
        override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) {
            mError = WebViewError(withError.code, withError.description.toString())
        }
    }

    actual fun goBack() {
        webview.value?.let {
            if (it.canGoBack) it.goBack()
        }
    }

    actual fun goForward() {
        webview.value?.let {
            if (it.canGoForward) it.goForward()
        }
    }

    actual fun evaluateJavaScript(script: String) {
        webview.value?.evaluateJavaScript(script, null)
    }
}