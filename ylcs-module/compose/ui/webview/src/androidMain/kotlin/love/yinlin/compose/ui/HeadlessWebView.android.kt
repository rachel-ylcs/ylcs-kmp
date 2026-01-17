package love.yinlin.compose.ui

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Stable
import love.yinlin.foundation.Context

@Stable
actual abstract class HeadlessWebView actual constructor(context: Context) {
    @SuppressLint("SetJavaScriptEnabled")
    private val webview = WebView(context.activity).apply {
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
            @Suppress("unused")
            fun onUrlDetected(url: String?): Boolean = url?.let { onUrlIntercepted(it) } ?: false

            @Suppress("unused")
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