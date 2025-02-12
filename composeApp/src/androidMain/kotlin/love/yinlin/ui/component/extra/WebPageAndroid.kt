package love.yinlin.ui.component.extra

import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.viewinterop.AndroidView
import love.yinlin.extension.UpdateAlways

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

@Composable
actual fun WebPage(
	state: WebPageState,
	modifier: Modifier
) {
	val client = remember(state) { object : WebViewClient() {
		override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
			super.onPageStarted(view, url, favicon)
			url?.let { state.mUrl = it }
			state.mLoadingState = WebPageLoadingState.Loading(0f)
			state.mTitle = ""
			favicon?.let { state.mIcon = BitmapPainter(it.asImageBitmap()) }
		}

		override fun onPageFinished(view: WebView?, url: String?) {
			super.onPageFinished(view, url)
			url?.let { state.mUrl = it }
			state.mLoadingState = WebPageLoadingState.Finished
		}

		override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
			super.doUpdateVisitedHistory(view, url, isReload)
			if (view != null) {
				state.mCanGoBack = view.canGoBack()
				state.mCanGoForward = view.canGoForward()
			}
		}

		override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
			super.onReceivedError(view, request, error)
			error?.let { state.mError = WebPageError(it.errorCode.toLong(), it.description.toString()) }
		}
	} }
	val chromeClient = remember(state) { object : WebChromeClient() {
		override fun onReceivedTitle(view: WebView?, title: String?) {
			super.onReceivedTitle(view, title)
			title?.let { state.mTitle = it }
		}

		override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
			super.onReceivedIcon(view, icon)
			icon?.let { state.mIcon = BitmapPainter(it.asImageBitmap()) }
		}

		override fun onProgressChanged(view: WebView?, newProgress: Int) {
			super.onProgressChanged(view, newProgress)
			state.mLoadingState = WebPageLoadingState.Loading(newProgress / 100f)
		}
	} }

	BackHandler(state.canGoBack) {
		state.update = UpdateWebPage.GoBack
	}

	AndroidView(
		modifier = modifier,
		factory = { context ->
			val webview = WebView(context)
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
			webview.webViewClient = client
			webview.webChromeClient = chromeClient
			webview
		},
		update = { webview -> when (val update = state.update) {
			is UpdateWebPage.LoadUrl -> {
				webview.loadUrl(update.url)
			}
			is UpdateWebPage.GoBack -> {
				if (webview.canGoBack()) webview.goBack()
			}
			is UpdateWebPage.GoForward -> {
				if (webview.canGoForward()) webview.goForward()
			}
			is UpdateWebPage.EvaluateJavaScript -> {
				webview.evaluateJavascript(update.script, null)
			}
			is UpdateWebPage.None -> {}
		} },
		onRelease = { webview ->
			webview.destroy()
		}
	)
}