package love.yinlin.compose.ui.platform

import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import love.yinlin.compose.mutableRefStateOf

@Stable
actual class WebViewState actual constructor(val settings: WebViewConfig, initUrl: String) {
    internal val webview = mutableRefStateOf<AndroidWebView?>(null)

    internal var mUrl: String by mutableStateOf(initUrl)
    actual var url: String get() = mUrl
        set(value) { webview.value?.loadUrl(value) }

    internal var mLoadingState: WebViewLoadingState by mutableRefStateOf(WebViewLoadingState.Initializing)
    actual val loadingState: WebViewLoadingState by derivedStateOf { mLoadingState }

    internal var mTitle: String by mutableStateOf("")
    actual val title: String by derivedStateOf { mTitle }

    internal var mIcon: BitmapPainter? by mutableRefStateOf(null)
    actual val icon: BitmapPainter? by derivedStateOf { mIcon }

    internal var mCanGoBack: Boolean by mutableStateOf(false)
    actual val canGoBack: Boolean by derivedStateOf { mCanGoBack }

    internal var mCanGoForward: Boolean by mutableStateOf(false)
    actual val canGoForward: Boolean by derivedStateOf { mCanGoForward }

    internal var mError: WebViewError? by mutableRefStateOf(null)
    actual val error: WebViewError? by derivedStateOf { mError }

    internal val client = object : WebViewClient() {
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

    internal val chromeClient = object : WebChromeClient() {
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
        webview.value?.let {
            if (it.canGoBack()) it.goBack()
        }
    }

    actual fun goForward() {
        webview.value?.let {
            if (it.canGoForward()) it.goForward()
        }
    }

    actual fun evaluateJavaScript(script: String) {
        webview.value?.evaluateJavascript(script, null)
    }
}