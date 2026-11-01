package love.yinlin.compose.ui

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import kotlinx.cinterop.ObjCSignatureOverride
import love.yinlin.compose.extension.mutableRefStateOf
import platform.Foundation.NSError
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.WebKit.*
import platform.darwin.NSObject

@Stable
actual class WebViewState actual constructor(initUrl: String) : PlatformViewState<WKWebView, WebViewParameters>() {
    private var stateUrl by mutableStateOf(initUrl)
    private var stateLoadingState: WebViewLoadingState by mutableRefStateOf(WebViewLoadingState.Initializing)
    private var stateTitle by mutableStateOf("")
    private var stateCanGoBack by mutableStateOf(false)
    private var stateCanGoForward by mutableStateOf(false)
    private var stateError: WebViewError? by mutableRefStateOf(null)
    private var requestedUrl = initUrl
    private var requestVersion = 0L
    private var appliedVersion = -1L
    private val delegates = mutableMapOf<WKWebView, WKNavigationDelegateProtocol>()
    private val observers = mutableMapOf<WKWebView, MacosWebViewObserver>()

    actual var url: String
        get() = stateUrl
        set(value) {
            requestedUrl = value
            stateUrl = value
            requestVersion++
            withView { navigate(it, requestedUrl) }
        }
    actual val loadingState: WebViewLoadingState get() = stateLoadingState
    actual val title: String get() = stateTitle
    actual val icon: BitmapPainter? = null
    actual val canGoBack: Boolean get() = stateCanGoBack
    actual val canGoForward: Boolean get() = stateCanGoForward
    actual val error: WebViewError? get() = stateError

    actual fun goBack() = withView { if (it.canGoBack) it.goBack() }
    actual fun goForward() = withView { if (it.canGoForward) it.goForward() }
    actual fun evaluateJavaScript(script: String) = withView { it.evaluateJavaScript(script, null) }

    override fun factory(environment: PlatformViewEnvironment): WKWebView = WKWebView().apply {
        allowsBackForwardNavigationGestures = true
    }

    override fun attach(view: WKWebView) {
        appliedVersion = -1L
        stateLoadingState = WebViewLoadingState.Initializing
        stateCanGoBack = false
        stateCanGoForward = false
        val delegate = object : NSObject(), WKNavigationDelegateProtocol {
            @ObjCSignatureOverride
            override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) = onView(webView) {
                stateError = null
                stateLoadingState = WebViewLoadingState.Loading(0f)
                update(it)
            }

            @ObjCSignatureOverride
            override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) = onView(webView) {
                update(it)
                stateLoadingState = WebViewLoadingState.Finished
            }

            @ObjCSignatureOverride
            override fun webView(webView: WKWebView, didFailProvisionalNavigation: WKNavigation?, withError: NSError) = failure(webView, withError)

            @ObjCSignatureOverride
            override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) = failure(webView, withError)

            override fun webViewWebContentProcessDidTerminate(webView: WKWebView) = onView(webView) {
                stateError = WebViewError(-1L, "The WebKit content process terminated")
                stateLoadingState = WebViewLoadingState.Finished
            }
        }
        delegates[view] = delegate
        view.navigationDelegate = delegate
        observers[view] = MacosWebViewObserver(view) { onView(it, ::update) }
    }

    override fun synchronize(view: WKWebView, previous: WebViewParameters?, next: WebViewParameters) {
        if (previous?.config?.enableJavaScript != next.config.enableJavaScript) view.configuration.defaultWebpagePreferences.allowsContentJavaScript = next.config.enableJavaScript
        if (previous?.config?.enableJavaScriptOpenWindow != next.config.enableJavaScriptOpenWindow) view.configuration.preferences.javaScriptCanOpenWindowsAutomatically = next.config.enableJavaScriptOpenWindow
        if (appliedVersion != requestVersion) navigate(view, requestedUrl)
    }

    private fun update(view: WKWebView) {
        view.URL?.absoluteString?.let {
            stateUrl = it
            requestedUrl = it
        }
        stateTitle = view.title.orEmpty()
        stateCanGoBack = view.canGoBack
        stateCanGoForward = view.canGoForward
        if (view.loading) stateLoadingState = WebViewLoadingState.Loading(view.estimatedProgress.toFloat().coerceIn(0f, 1f))
    }

    private fun navigate(view: WKWebView, target: String) {
        appliedVersion = requestVersion
        val address = NSURL.URLWithString(target.ifEmpty { "about:blank" })
        if (address != null) view.loadRequest(NSMutableURLRequest(address))
        else {
            stateError = WebViewError(-1L, "Invalid URL")
            stateLoadingState = WebViewLoadingState.Finished
        }
    }

    private fun failure(view: WKWebView, error: NSError) = onView(view) {
        if (error.code != -999L) {
            update(it)
            stateError = WebViewError(error.code, error.localizedDescription)
            stateLoadingState = WebViewLoadingState.Finished
        }
    }

    override fun detach(view: WKWebView) {
        cleanup(
            { observers.remove(view)?.close() },
            { view.navigationDelegate = null },
            view::stopLoading,
            view::removeFromSuperview,
            { delegates.remove(view) }
        )
    }

    override fun releaseResources() {
        observers.values.forEach { it.close() }
        observers.clear()
        delegates.clear()
    }
}
