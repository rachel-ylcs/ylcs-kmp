package love.yinlin.ui.component.platform

import android.graphics.Bitmap
import android.graphics.Color
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import love.yinlin.ui.CustomUI

@Stable
actual class WebPageState actual constructor(val settings: WebPageSettings, initUrl: String) {
	internal val webview = mutableStateOf<WebView?>(null)

	internal var mUrl: String by mutableStateOf(initUrl)
	actual var url: String get() = mUrl
		set(value) { webview.value?.loadUrl(value) }

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

	internal val client = object : WebViewClient() {
		override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
			super.onPageStarted(view, url, favicon)
			url?.let { mUrl = it }
			mLoadingState = WebPageLoadingState.Loading(0f)
			mTitle = ""
			favicon?.let { mIcon = BitmapPainter(it.asImageBitmap()) }
		}

		override fun onPageFinished(view: WebView?, url: String?) {
			super.onPageFinished(view, url)
			url?.let { mUrl = it }
			mLoadingState = WebPageLoadingState.Finished
		}

		override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
			super.doUpdateVisitedHistory(view, url, isReload)
			if (view != null) {
				mCanGoBack = view.canGoBack()
				mCanGoForward = view.canGoForward()
			}
		}

		override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
			super.onReceivedError(view, request, error)
			error?.let { mError = WebPageError(it.errorCode.toLong(), it.description.toString()) }
		}
	}

	internal val chromeClient = object : WebChromeClient() {
		override fun onReceivedTitle(view: WebView?, title: String?) {
			super.onReceivedTitle(view, title)
			title?.let { mTitle = it }
		}

		override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
			super.onReceivedIcon(view, icon)
			icon?.let { mIcon = BitmapPainter(it.asImageBitmap()) }
		}

		override fun onProgressChanged(view: WebView?, newProgress: Int) {
			super.onProgressChanged(view, newProgress)
			mLoadingState = WebPageLoadingState.Loading(newProgress / 100f)
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

@Composable
actual fun WebPage(
	state: WebPageState,
	modifier: Modifier
) {
	BackHandler(state.canGoBack) {
		state.goBack()
	}

	CustomUI(
		view = state.webview,
		modifier = modifier,
		factory = { context ->
			val webview = WebView(context)
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