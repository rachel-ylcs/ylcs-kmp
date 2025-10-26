package love.yinlin.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.platform.PlatformView
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.WebKit.WKWebView
import platform.WebKit.javaScriptEnabled

@Composable
actual fun WebView(
    state: WebViewState,
    modifier: Modifier
) {
    PlatformView(
        view = state.webview,
        modifier = modifier,
        factory = {
            val webview = WKWebView()
            webview.configuration.apply {
                defaultWebpagePreferences.allowsContentJavaScript = state.settings.enableJavaScript
                preferences.javaScriptEnabled = state.settings.enableJavaScript
                preferences.javaScriptCanOpenWindowsAutomatically = state.settings.enableJavaScriptOpenWindow
            }
            webview.setUserInteractionEnabled(true)
            webview.allowsBackForwardNavigationGestures = true
            webview.navigationDelegate = state.protocol
            if (state.mUrl.isNotEmpty()) webview.loadRequest(NSMutableURLRequest(NSURL(string = state.mUrl)))
            webview
        }
    )
}