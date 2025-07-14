package love.yinlin.ui.component.platform

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import love.yinlin.platform.appNative
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

@Stable
actual abstract class HeadlessBrowser actual constructor() {
	@SuppressLint("SetJavaScriptEnabled")
    private val webview = WebView(appNative.context).apply {
        visibility = View.GONE
        layoutParams = ViewGroup.LayoutParams(1, 1)
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        settings.apply {
			javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = false
            cacheMode = WebSettings.LOAD_NO_CACHE
            domStorageEnabled = false
			allowFileAccess = false
			allowContentAccess = false
			blockNetworkImage = true
			blockNetworkLoads = false
			loadsImagesAutomatically = false
			useWideViewPort = false
			builtInZoomControls = false
			displayZoomControls = false
			mediaPlaybackRequiresUserGesture = true
            safeBrowsingEnabled = false
			userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 Edg/138.0.0.0"
			setSupportMultipleWindows(false)
			setSupportZoom(false)
			setGeolocationEnabled(true)
            setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_WAIVED, true)
		}
		webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url.toString()
                return if (url.endsWith(".jpg") || url.endsWith(".png") || url.endsWith(".webp")) {
                    WebResourceResponse(null, null, null)
                } else super.shouldInterceptRequest(view, request)
            }

			override fun onPageFinished(view: WebView?, url: String?) {
				view?.evaluateJavascript("""
(function() {
	var originalOpen = XMLHttpRequest.prototype.open;
	XMLHttpRequest.prototype.open = function(method, url) {
		this._url = url;
		originalOpen.apply(this, arguments);
	};
	var originalSend = XMLHttpRequest.prototype.send;
	XMLHttpRequest.prototype.send = function(body) {
		var self = this;
		this.addEventListener('readystatechange', function() {
			if (self.readyState === 4 && self.status === 200) {
				try {
					if (RequestInterceptor.onUrlDetected(self._url)) {
						RequestInterceptor.onRequestDetected(self._url, self.responseText);
					}
				} catch(e) { }
			}
		});
		originalSend.apply(this, arguments);
	};
})();
""", null)
			}
		}
		webChromeClient = object : WebChromeClient() {}
		addJavascriptInterface(object {
			@JavascriptInterface
			fun onUrlDetected(url: String?): Boolean = url?.let { onUrlIntercepted(it) } ?: false

			@JavascriptInterface
			fun onRequestDetected(url: String?, json: String?) {
				if (url != null && json != null) {
                    if (onRequestIntercepted(url, json)) stopLoading()
                }
			}
		}, "RequestInterceptor")
	}

	private var isDestroy: Boolean = false

	actual fun load(url: String) = webview.loadUrl(url)

	actual fun destroy() {
		if (!isDestroy) {
			isDestroy = true
			webview.destroy()
		}
	}

	actual abstract fun onUrlIntercepted(url: String): Boolean

	actual abstract fun onRequestIntercepted(url: String, response: String): Boolean
}