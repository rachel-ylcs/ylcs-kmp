package love.yinlin.ui.component.extra

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import kotlinx.cinterop.ObjCSignatureOverride
import platform.Foundation.NSError
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.javaScriptEnabled
import platform.darwin.NSObject
import love.yinlin.extension.UpdateAlways
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL

@Stable
sealed interface UpdateWebPage {
	data object None : UpdateWebPage
	data class LoadUrl(val url: String) : UpdateWebPage
	object GoBack : UpdateWebPage, UpdateAlways()
	object GoForward : UpdateWebPage, UpdateAlways()
	data class EvaluateJavaScript(val script: String) : UpdateWebPage
}

@Stable
actual class WebPageState actual constructor(val settings: WebPageSettings) {
	internal var update: UpdateWebPage by mutableStateOf(UpdateWebPage.None)

	internal var mUrl: String by mutableStateOf("")
	actual var url: String get() = mUrl
		set(value) { update = UpdateWebPage.LoadUrl(value) }

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

	actual fun goBack() { update = UpdateWebPage.GoBack }
	actual fun goForward() { update = UpdateWebPage.GoForward }
	actual fun evaluateJavaScript(script: String) { update = UpdateWebPage.EvaluateJavaScript(script) }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun WebPage(
	state: WebPageState,
	modifier: Modifier
) {
	val protocol = remember(state) { object : NSObject(), WKNavigationDelegateProtocol {
		@ObjCSignatureOverride
		override fun webView(webview: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
			webview.URL?.absoluteString?.let { state.mUrl = it }
			state.mLoadingState = WebPageLoadingState.Loading(0f)
			webview.title?.let { state.mTitle = it }
		}

		@ObjCSignatureOverride
		override fun webView(webview: WKWebView, didFinishNavigation: WKNavigation?) {
			webview.URL?.absoluteString?.let { state.mUrl = it }
			state.mLoadingState = WebPageLoadingState.Finished
			webview.title?.let { state.mTitle = it }
		}

		@ObjCSignatureOverride
		override fun webView(webview: WKWebView, didCommitNavigation: WKNavigation?) {
			state.mLoadingState = WebPageLoadingState.Loading(webview.estimatedProgress.toFloat())
		}

		@ObjCSignatureOverride
		override fun webView(webview: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) {
			state.mError = WebPageError(withError.code, withError.description.toString())
		}
	} }

	UIKitView(
		modifier = modifier,
		properties = UIKitInteropProperties(
			interactionMode = UIKitInteropInteractionMode.NonCooperative
		),
		factory = {
			val webview = WKWebView()
			webview.configuration.apply {
				defaultWebpagePreferences.allowsContentJavaScript = state.settings.enableJavaScript
				preferences.javaScriptEnabled = state.settings.enableJavaScript
				preferences.javaScriptCanOpenWindowsAutomatically = state.settings.enableJavaScriptOpenWindow
			}
			webview.setUserInteractionEnabled(true)
			webview.allowsBackForwardNavigationGestures = true
			webview.navigationDelegate = protocol
			webview
		},
		update = { webview -> when (val update = state.update) {
			is UpdateWebPage.LoadUrl -> {
				webview.loadRequest(NSMutableURLRequest(NSURL(string = update.url)))
			}
			is UpdateWebPage.GoBack -> {
				if (webview.canGoBack()) webview.goBack()
			}
			is UpdateWebPage.GoForward -> {
				if (webview.canGoForward()) webview.goForward()
			}
			is UpdateWebPage.EvaluateJavaScript -> {
				webview.evaluateJavaScript(update.script, null)
			}
			is UpdateWebPage.None -> {}
		} }
	)
}