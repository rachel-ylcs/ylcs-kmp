package love.yinlin.compose.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView as AndroidWebView
import android.webkit.WebViewClient
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import love.yinlin.compose.extension.mutableRefStateOf

@Stable
actual class WebViewState actual constructor(initUrl: String) : PlatformViewState<AndroidWebView, WebViewParameters>() {
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
    private var savedState: Bundle? = null
    private var latestView: AndroidWebView? = null

    actual var url: String
        get() = stateUrl
        set(value) {
            requestedUrl = value
            requestVersion++
            withView(::navigate)
        }
    actual val loadingState: WebViewLoadingState get() = stateLoadingState
    actual val title: String get() = stateTitle
    actual val icon: BitmapPainter? get() = stateIcon
    actual val canGoBack: Boolean get() = stateCanGoBack
    actual val canGoForward: Boolean get() = stateCanGoForward
    actual val error: WebViewError? get() = stateError

    actual fun goBack() = withView { if (it.canGoBack()) it.goBack() }
    actual fun goForward() = withView { if (it.canGoForward()) it.goForward() }
    actual fun evaluateJavaScript(script: String) = withView { it.evaluateJavascript(script, null) }

    override fun factory(environment: PlatformViewEnvironment): AndroidWebView = AndroidWebView(environment.context)

    override fun attach(view: AndroidWebView) {
        latestView = view
        view.setBackgroundColor(Color.TRANSPARENT)
        view.settings.apply {
            blockNetworkImage = false
            blockNetworkLoads = false
            loadsImagesAutomatically = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(true)
            setSupportZoom(true)
            setGeolocationEnabled(true)
        }
        view.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: AndroidWebView?, url: String?, favicon: Bitmap?) {
                if (view != null) onView(view) {
                    url?.let { stateUrl = it }
                    stateLoadingState = WebViewLoadingState.Loading(0f)
                    stateTitle = ""
                    stateError = null
                    stateIcon = favicon?.let { BitmapPainter(it.asImageBitmap()) }
                }
            }

            override fun onPageFinished(view: AndroidWebView?, url: String?) {
                if (view != null) onView(view) {
                    url?.let { stateUrl = it }
                    stateLoadingState = WebViewLoadingState.Finished
                    stateCanGoBack = it.canGoBack()
                    stateCanGoForward = it.canGoForward()
                }
            }

            override fun doUpdateVisitedHistory(view: AndroidWebView?, url: String?, isReload: Boolean) {
                if (view != null) onView(view) {
                    stateCanGoBack = it.canGoBack()
                    stateCanGoForward = it.canGoForward()
                }
            }

            override fun onReceivedError(view: AndroidWebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (view != null && request?.isForMainFrame == true && error != null) onView(view) {
                    stateError = WebViewError(error.errorCode.toLong(), error.description.toString())
                }
            }
        }
        view.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: AndroidWebView?, title: String?) {
                if (view != null && title != null) onView(view) { stateTitle = title }
            }

            override fun onReceivedIcon(view: AndroidWebView?, icon: Bitmap?) {
                if (view != null && icon != null) onView(view) { stateIcon = BitmapPainter(icon.asImageBitmap()) }
            }

            override fun onProgressChanged(view: AndroidWebView?, newProgress: Int) {
                if (view != null) onView(view) { stateLoadingState = WebViewLoadingState.Loading(newProgress / 100f) }
            }
        }
    }

    override fun synchronize(view: AndroidWebView, previous: WebViewParameters?, next: WebViewParameters) {
        val before = previous?.config
        val config = next.config
        view.settings.apply {
            if (before?.enableJavaScript != config.enableJavaScript) javaScriptEnabled = config.enableJavaScript
            if (before?.enableJavaScriptOpenWindow != config.enableJavaScriptOpenWindow) javaScriptCanOpenWindowsAutomatically = config.enableJavaScriptOpenWindow
            if (before?.enableDomStorage != config.enableDomStorage) domStorageEnabled = config.enableDomStorage
            if (before?.enableFileAccess != config.enableFileAccess) allowFileAccess = config.enableFileAccess
            if (before?.enableContentAccess != config.enableContentAccess) allowContentAccess = config.enableContentAccess
        }
        if (previous == null) {
            val snapshot = savedState
            savedState = null
            val restored = snapshot != null && appliedVersion == requestVersion && view.restoreState(snapshot) != null
            if (!restored) {
                val target = if (appliedVersion != requestVersion) requestedUrl else stateUrl.ifEmpty { requestedUrl }
                if (target.isNotEmpty()) view.loadUrl(target)
                appliedVersion = requestVersion
            }
        }
        else navigate(view)
    }

    private fun navigate(view: AndroidWebView) {
        if (appliedVersion != requestVersion) {
            view.loadUrl(requestedUrl)
            appliedVersion = requestVersion
        }
    }

    override fun detach(view: AndroidWebView) {
        cleanup({
            if (latestView === view) {
                latestView = null
                view.url?.let { stateUrl = it }
                savedState = Bundle().also { view.saveState(it) }
            }
        }, { view.stopLoading() }, { view.webChromeClient = null }, { view.webViewClient = WebViewClient() }, {
            (view.parent as? ViewGroup)?.removeView(view)
        }, view::destroy)
    }

    override fun releaseResources() {
        savedState = null
        latestView = null
        stateIcon = null
    }
}
