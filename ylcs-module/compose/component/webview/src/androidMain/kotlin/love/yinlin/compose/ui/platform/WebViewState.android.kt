package love.yinlin.compose.ui.platform

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
import love.yinlin.compose.mutableRefStateOf
import love.yinlin.platform.PlatformView

private typealias AndroidWebView = android.webkit.WebView

@Stable
actual class WebViewState actual constructor(val settings: WebViewConfig, initUrl: String) : PlatformView<AndroidWebView>() {
    private var mUrl: String by mutableStateOf(initUrl)
    actual var url: String get() = mUrl
        set(value) { view?.loadUrl(value) }

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

    private val client = object : WebViewClient() {
        override fun onPageStarted(view: AndroidWebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            url?.let { mUrl = it }
            mLoadingState = WebViewLoadingState.Loading(0f)
            mTitle = ""
            favicon?.let { mIcon = BitmapPainter(it.asImageBitmap()) }
        }

        override fun onPageFinished(view: AndroidWebView?, url: String?) {
            super.onPageFinished(view, url)
            url?.let { mUrl = it }
            mLoadingState = WebViewLoadingState.Finished
        }

        override fun doUpdateVisitedHistory(view: AndroidWebView?, url: String?, isReload: Boolean) {
            super.doUpdateVisitedHistory(view, url, isReload)
            if (view != null) {
                mCanGoBack = view.canGoBack()
                mCanGoForward = view.canGoForward()
            }
        }

        override fun onReceivedError(view: AndroidWebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            error?.let { mError = WebViewError(it.errorCode.toLong(), it.description.toString()) }
        }
    }

    private val chromeClient = object : WebChromeClient() {
        override fun onReceivedTitle(view: AndroidWebView?, title: String?) {
            super.onReceivedTitle(view, title)
            title?.let { mTitle = it }
        }

        override fun onReceivedIcon(view: AndroidWebView?, icon: Bitmap?) {
            super.onReceivedIcon(view, icon)
            icon?.let { mIcon = BitmapPainter(it.asImageBitmap()) }
        }

        override fun onProgressChanged(view: AndroidWebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            mLoadingState = WebViewLoadingState.Loading(newProgress / 100f)
        }
    }

    actual fun goBack() {
        view?.let {
            if (it.canGoBack()) it.goBack()
        }
    }

    actual fun goForward() {
        view?.let {
            if (it.canGoForward()) it.goForward()
        }
    }

    actual fun evaluateJavaScript(script: String) {
        view?.evaluateJavascript(script, null)
    }

    override fun build(context: Context, lifecycleOwner: LifecycleOwner, activityResultRegistry: ActivityResultRegistry?): AndroidWebView {
        val webView = AndroidWebView(context)
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.settings.apply {
            javaScriptEnabled = settings.enableJavaScript
            javaScriptCanOpenWindowsAutomatically = settings.enableJavaScriptOpenWindow
            domStorageEnabled = settings.enableDomStorage
            allowFileAccess = settings.enableFileAccess
            allowContentAccess = settings.enableContentAccess

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
        if (mUrl.isNotEmpty()) webView.loadUrl(mUrl)
        return webView
    }

    override fun release(view: AndroidWebView) {
        view.destroy()
    }
}