package love.yinlin.compose.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import androidx.activity.result.ActivityResultRegistry
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.lifecycle.LifecycleOwner
import love.yinlin.compose.extension.mutableRefStateOf

internal typealias AndroidWebView = android.webkit.WebView

@Stable
actual class WebViewState actual constructor(private val initUrl: String) : PlatformView<AndroidWebView>(), Releasable<AndroidWebView> {
    internal var stateUrl: String by mutableStateOf("")
    actual var url: String get() = stateUrl
        set(value) { host?.loadUrl(value) }

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

    actual fun goBack() {
        host?.let {
            if (it.canGoBack()) it.goBack()
        }
    }

    actual fun goForward() {
        host?.let {
            if (it.canGoForward()) it.goForward()
        }
    }

    actual fun evaluateJavaScript(script: String) {
        host?.evaluateJavascript(script, null)
    }

    val client = object : WebViewClient() {
        override fun onPageStarted(view: AndroidWebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            url?.let { stateUrl = it }
            stateLoadingState = WebViewLoadingState.Loading(0f)
            stateTitle = ""
            favicon?.let { stateIcon = BitmapPainter(it.asImageBitmap()) }
        }

        override fun onPageFinished(view: AndroidWebView?, url: String?) {
            super.onPageFinished(view, url)
            url?.let { stateUrl = it }
            stateLoadingState = WebViewLoadingState.Finished
        }

        override fun doUpdateVisitedHistory(view: AndroidWebView?, url: String?, isReload: Boolean) {
            super.doUpdateVisitedHistory(view, url, isReload)
            if (view != null) {
                stateCanGoBack = view.canGoBack()
                stateCanGoForward = view.canGoForward()
            }
        }

        override fun onReceivedError(view: AndroidWebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            error?.let { stateError = WebViewError(it.errorCode.toLong(), it.description.toString()) }
        }
    }

    val chromeClient = object : WebChromeClient() {
        override fun onReceivedTitle(view: AndroidWebView?, title: String?) {
            super.onReceivedTitle(view, title)
            title?.let { stateTitle = it }
        }

        override fun onReceivedIcon(view: AndroidWebView?, icon: Bitmap?) {
            super.onReceivedIcon(view, icon)
            icon?.let { stateIcon = BitmapPainter(it.asImageBitmap()) }
        }

        override fun onProgressChanged(view: AndroidWebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            stateLoadingState = WebViewLoadingState.Loading(newProgress / 100f)
        }
    }

    override fun build(context: Context, lifecycleOwner: LifecycleOwner, activityResultRegistry: ActivityResultRegistry?): AndroidWebView {
        val webView = AndroidWebView(context)
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.settings.apply {
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
        webView.webViewClient = client
        webView.webChromeClient = chromeClient
        if (initUrl.isNotEmpty()) webView.loadUrl(initUrl)
        return webView
    }

    override fun release(view: AndroidWebView) {
        view.destroy()
    }
}