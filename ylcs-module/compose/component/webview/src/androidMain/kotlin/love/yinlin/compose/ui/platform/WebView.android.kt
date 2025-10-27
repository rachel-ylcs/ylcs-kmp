package love.yinlin.compose.ui.platform

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.compose.ui.layout.NavigationBack
import love.yinlin.platform.PlatformView

typealias AndroidWebView = android.webkit.WebView

@Composable
actual fun WebView(
    state: WebViewState,
    modifier: Modifier
) {
    NavigationBack(enabled = state.canGoBack) {
        state.goBack()
    }

    PlatformView(
        view = state.webview,
        modifier = modifier,
        factory = { context ->
            val webview = AndroidWebView(context)
            webview.setBackgroundColor(Color.TRANSPARENT)
            webview.settings.apply {
                javaScriptEnabled = state.settings.enableJavaScript
                javaScriptCanOpenWindowsAutomatically = state.settings.enableJavaScriptOpenWindow
                domStorageEnabled = state.settings.enableDomStorage
                allowFileAccess = state.settings.enableFileAccess
                allowContentAccess = state.settings.enableContentAccess

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
            webview.webViewClient = state.client
            webview.webChromeClient = state.chromeClient
            if (state.mUrl.isNotEmpty()) webview.loadUrl(state.mUrl)
            webview
        },
        release = { webview, onRelease ->
            webview.destroy()
            onRelease()
        }
    )
}