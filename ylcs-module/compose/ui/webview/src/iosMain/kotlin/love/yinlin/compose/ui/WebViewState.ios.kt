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
import platform.WebKit.javaScriptEnabled
import platform.darwin.NSObject

@Stable
actual class WebViewState actual constructor(val settings: WebViewConfig, initUrl: String) : PlatformView<WKWebView>() {
    private var mUrl: String by mutableStateOf(initUrl)
    actual var url: String get() = mUrl
        set(value) { view?.loadRequest(NSMutableURLRequest(NSURL(string = value))) }

    private var mLoadingState: WebViewLoadingState by mutableRefStateOf(WebViewLoadingState.Initializing)
    actual val loadingState: WebViewLoadingState by derivedStateOf { mLoadingState }

    private var mTitle: String by mutableStateOf("")
    actual val title: String by derivedStateOf { mTitle }

    private var mIcon: BitmapPainter? by mutableRefStateOf(null)
    actual val icon: BitmapPainter? by derivedStateOf { mIcon }

    private var mCanGoBack: Boolean by mutableStateOf(false)
    actual val canGoBack: Boolean by derivedStateOf { mCanGoBack }

    private var mCanGoForward: Boolean by mutableStateOf(false)
    actual val canGoForward: Boolean by derivedStateOf { mCanGoForward }

    private var mError: WebViewError? by mutableRefStateOf(null)
    actual val error: WebViewError? by derivedStateOf { mError }

    private val protocol: WKNavigationDelegateProtocol = object : NSObject(), WKNavigationDelegateProtocol {
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
        view?.let {
            if (it.canGoBack) it.goBack()
        }
    }

    actual fun goForward() {
        view?.let {
            if (it.canGoForward) it.goForward()
        }
    }

    actual fun evaluateJavaScript(script: String) {
        view?.evaluateJavaScript(script, null)
    }

    override fun build(): WKWebView {
        val webview = WKWebView()
        webview.configuration.apply {
            defaultWebpagePreferences.allowsContentJavaScript = settings.enableJavaScript
            preferences.javaScriptEnabled = settings.enableJavaScript
            preferences.javaScriptCanOpenWindowsAutomatically = settings.enableJavaScriptOpenWindow
        }
        webview.setUserInteractionEnabled(true)
        webview.allowsBackForwardNavigationGestures = true
        webview.navigationDelegate = protocol
        if (mUrl.isNotEmpty()) webview.loadRequest(NSMutableURLRequest(NSURL(string = mUrl)))
        return webview
    }
}