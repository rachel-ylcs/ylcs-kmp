package love.yinlin.ui.component.extra

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import kotlinx.cinterop.ObjCSignatureOverride
import love.yinlin.extension.Reference
import platform.Foundation.NSError
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.javaScriptEnabled
import platform.darwin.NSObject
import love.yinlin.ui.CustomUI
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL

@Stable
actual class WebPageState actual constructor(val settings: WebPageSettings) {
	internal val webview = Reference<WKWebView>()

	internal var mUrl: String by mutableStateOf("")
	actual var url: String get() = mUrl
		set(value) { webview.value?.loadRequest(NSMutableURLRequest(NSURL(string = value))) }

	internal var mLoadingState: WebPageLoadingState by mutableStateOf(WebPageLoadingState.Initializing)
	actual val loadingState: WebPageLoadingState get() = mLoadingState

	internal var mTitle: String by mutableStateOf("")
	actual val title: String get() = mTitle

	internal var mIcon: BitmapPainter? by mutableStateOf(null)
	actual val icon: BitmapPainter? get() = mIcon

	internal var mCanGoBack: Boolean by mutableStateOf(false)
	actual val canGoBack: Boolean get() = mCanGoBack

	internal var mCanGoForward: Boolean by mutableStateOf(false)
	actual val canGoForward: Boolean get() = mCanGoForward

	internal var mError: WebPageError? by mutableStateOf(null)
	actual val error: WebPageError? get() = mError

	internal val protocol: WKNavigationDelegateProtocol = object : NSObject(), WKNavigationDelegateProtocol {
		@ObjCSignatureOverride
		override fun webView(webview: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
			webview.URL?.absoluteString?.let { mUrl = it }
			mLoadingState = WebPageLoadingState.Loading(0f)
			webview.title?.let { mTitle = it }
		}

		@ObjCSignatureOverride
		override fun webView(webview: WKWebView, didFinishNavigation: WKNavigation?) {
			webview.URL?.absoluteString?.let { mUrl = it }
			mLoadingState = WebPageLoadingState.Finished
			webview.title?.let { mTitle = it }
		}

		@ObjCSignatureOverride
		override fun webView(webview: WKWebView, didCommitNavigation: WKNavigation?) {
			mLoadingState = WebPageLoadingState.Loading(webview.estimatedProgress.toFloat())
		}

		@ObjCSignatureOverride
		override fun webView(webview: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) {
			mError = WebPageError(withError.code, withError.description.toString())
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

@Composable
actual fun WebPage(
	state: WebPageState,
	modifier: Modifier
) {
	CustomUI(
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
			webview
		}
	)
}