package love.yinlin.compose.ui

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import kotlinx.cinterop.ObjCSignatureOverride
import love.yinlin.compose.extension.mutableRefStateOf
import platform.Foundation.NSError
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.WebKit.*
import platform.darwin.NSObject

@Stable
actual class WebViewState actual constructor(initUrl: String) : PlatformViewState<WKWebView, WebViewParameters>() {
    private var stateUrl by mutableStateOf("")
    private var stateLoadingState: WebViewLoadingState by mutableRefStateOf(WebViewLoadingState.Initializing)
    private var stateTitle by mutableStateOf("")
    private var stateIcon: BitmapPainter? by mutableRefStateOf(null)
    private var stateCanGoBack by mutableStateOf(false)
    private var stateCanGoForward by mutableStateOf(false)
    private var stateError: WebViewError? by mutableRefStateOf(null)
    private var requestedUrl = initUrl
    private var requestVersion = 0L
    private var appliedVersion = -1L
    private val delegates = mutableMapOf<WKWebView, WKNavigationDelegateProtocol>()
    private var latestView: WKWebView? = null

    override val properties = UIKitInteropProperties(interactionMode = UIKitInteropInteractionMode.NonCooperative)

    actual var url: String
        get() = stateUrl
        set(value) {
            requestedUrl = value
            requestVersion++
            withView { navigate(it, requestedUrl) }
        }
    actual val loadingState: WebViewLoadingState get() = stateLoadingState
    actual val title: String get() = stateTitle
    actual val icon: BitmapPainter? get() = stateIcon
    actual val canGoBack: Boolean get() = stateCanGoBack
    actual val canGoForward: Boolean get() = stateCanGoForward
    actual val error: WebViewError? get() = stateError

    actual fun goBack() = withView { if (it.canGoBack) it.goBack() }
    actual fun goForward() = withView { if (it.canGoForward) it.goForward() }
    actual fun evaluateJavaScript(script: String) = withView { it.evaluateJavaScript(script, null) }

    override fun factory(environment: PlatformViewEnvironment): WKWebView = WKWebView().apply {
        setUserInteractionEnabled(true)
        allowsBackForwardNavigationGestures = true
    }

    override fun attach(view: WKWebView) {
        latestView = view
        val delegate = object : NSObject(), WKNavigationDelegateProtocol {
            @ObjCSignatureOverride
            override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) = onView(webView) {
                it.URL?.absoluteString?.let { value -> stateUrl = value }
                stateLoadingState = WebViewLoadingState.Loading(0f)
                stateError = null
                stateTitle = it.title.orEmpty()
            }

            @ObjCSignatureOverride
            override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) = onView(webView) {
                it.URL?.absoluteString?.let { value -> stateUrl = value }
                stateLoadingState = WebViewLoadingState.Finished
                stateTitle = it.title.orEmpty()
                stateCanGoBack = it.canGoBack
                stateCanGoForward = it.canGoForward
            }

            @ObjCSignatureOverride
            override fun webView(webView: WKWebView, didCommitNavigation: WKNavigation?) = onView(webView) {
                stateLoadingState = WebViewLoadingState.Loading(it.estimatedProgress.toFloat())
            }

            @ObjCSignatureOverride
            override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) = onView(webView) {
                stateError = WebViewError(withError.code, withError.description.toString())
            }
        }
        delegates[view] = delegate
        view.navigationDelegate = delegate
    }

    override fun synchronize(view: WKWebView, previous: WebViewParameters?, next: WebViewParameters) {
        val before = previous?.config
        val config = next.config
        view.configuration.apply {
            if (before?.enableJavaScript != config.enableJavaScript) {
                defaultWebpagePreferences.allowsContentJavaScript = config.enableJavaScript
                preferences.javaScriptEnabled = config.enableJavaScript
            }
            if (before?.enableJavaScriptOpenWindow != config.enableJavaScriptOpenWindow) preferences.javaScriptCanOpenWindowsAutomatically = config.enableJavaScriptOpenWindow
        }
        if (previous == null || appliedVersion != requestVersion) {
            val target = if (appliedVersion != requestVersion) requestedUrl else stateUrl.ifEmpty { requestedUrl }
            if (target.isNotEmpty()) navigate(view, target)
        }
    }

    private fun navigate(view: WKWebView, target: String) {
        view.loadRequest(NSMutableURLRequest(NSURL(string = target)))
        appliedVersion = requestVersion
    }

    override fun detach(view: WKWebView) {
        cleanup({
            if (latestView === view) {
                latestView = null
                view.URL?.absoluteString?.let { stateUrl = it }
            }
        }, { view.navigationDelegate = null }, view::stopLoading,
            view::removeFromSuperview, { delegates.remove(view) })
    }

    override fun releaseResources() {
        delegates.clear()
        latestView = null
        stateIcon = null
    }
}
