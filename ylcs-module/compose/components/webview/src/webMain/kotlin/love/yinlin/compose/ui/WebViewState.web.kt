@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compose.ui

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.Job
import kotlinx.coroutines.await
import kotlinx.coroutines.job
import love.yinlin.compose.graphics.decode
import love.yinlin.extension.asByteArray
import love.yinlin.extension.catching
import love.yinlin.extension.catchingError
import love.yinlin.extension.catchingNull
import love.yinlin.extension.createElement
import org.w3c.dom.Document
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.MutationObserver
import org.w3c.dom.Window
import org.w3c.dom.url.URL

@Stable
actual class WebViewState actual constructor(initUrl: String) : PlatformViewState<HTMLIFrameElement, WebViewParameters>() {
    private var currentUrl by mutableStateOf(initUrl)
    private var observedDocument: Document? = null
    private var unregisterDocument: (() -> Unit)? = null
    private var invalidUrl = false
    private var faviconUrl: String? = null
    private var faviconJob: Job? = null

    actual var url: String
        get() = currentUrl
        set(value) {
            currentUrl = value
            withView { navigate(it, value) }
        }

    private var stateLoadingState: WebViewLoadingState by mutableStateOf(WebViewLoadingState.Initializing)
    private var stateTitle: String by mutableStateOf("")
    private var stateIcon: BitmapPainter? by mutableStateOf(null)
    private var stateCanGoBack: Boolean by mutableStateOf(false)
    private var stateCanGoForward: Boolean by mutableStateOf(false)
    private var stateError: WebViewError? by mutableStateOf(null)

    actual val loadingState: WebViewLoadingState get() = stateLoadingState
    actual val title: String get() = stateTitle
    actual val icon: BitmapPainter? get() = stateIcon
    actual val canGoBack: Boolean get() = stateCanGoBack
    actual val canGoForward: Boolean get() = stateCanGoForward
    actual val error: WebViewError? get() = stateError

    actual fun goBack() = traverse(back = true)
    actual fun goForward() = traverse(back = false)

    actual fun evaluateJavaScript(script: String) = withView { view ->
        val child = observedDocument ?: return@withView
        if (!parameters.config.enableJavaScript || view.contentDocument !== child) return@withView
        val frame = view.contentWindow ?: return@withView
        catchingError { frameEvaluate(frame, script) }?.let { failure ->
            stateError = WebViewError(0L, failure.message ?: "JavaScript evaluation failed")
        }
    }

    override fun factory(environment: PlatformViewEnvironment): HTMLIFrameElement = createElement {
        frameBorder = "0"
        referrerPolicy = "no-referrer"
    }

    override fun attach(view: HTMLIFrameElement) {
        view.onload = { onView(view, ::onLoad) }
    }

    override fun synchronize(view: HTMLIFrameElement, previous: WebViewParameters?, next: WebViewParameters) {
        val sandbox = buildString {
            append("allow-forms allow-downloads")
            if (next.config.enableJavaScript) append(" allow-scripts")
            if (next.config.enableJavaScriptOpenWindow) append(" allow-popups")
            if (next.config.enableDomStorage) append(" allow-same-origin")
        }
        if (view.getAttribute("sandbox") != sandbox) {
            view.setAttribute("sandbox", sandbox)
            navigate(view, currentUrl, reload = true)
        }
    }

    override fun detach(view: HTMLIFrameElement) {
        view.onload = null
        clearDocument()
        view.src = "about:blank"
        stateLoadingState = WebViewLoadingState.Initializing
    }

    private fun navigate(view: HTMLIFrameElement, target: String, reload: Boolean = false) {
        val resolved = catchingNull { URL(target.ifBlank { "about:blank" }, document.baseURI) }
        if (resolved == null || (resolved.protocol != "http:" && resolved.protocol != "https:" && resolved.href != "about:blank")) {
            clearDocument()
            invalidUrl = true
            stateError = WebViewError(0L, "The WebView URL must use HTTP or HTTPS")
            stateLoadingState = WebViewLoadingState.Finished
            view.src = "about:blank"
            return
        }
        val child = observedDocument
        val frame = view.contentWindow
        if (!reload && child != null && frame != null && view.contentDocument === child && child.URL != resolved.href && child.URL.substringBefore('#') == resolved.href.substringBefore('#')) {
            frame.location.href = resolved.href
            return
        }
        clearDocument()
        invalidUrl = false
        stateError = null
        stateLoadingState = WebViewLoadingState.Loading(0f)
        view.src = resolved.href
    }

    private fun onLoad(view: HTMLIFrameElement) {
        if (invalidUrl) return
        val child = view.contentDocument
        // 插入 iframe 时，浏览器可能先触发初始空白文档的 load。
        if (observedDocument == null && child?.URL == "about:blank" && view.getAttribute("src") != "about:blank") return
        clearDocument()
        stateError = null
        stateLoadingState = WebViewLoadingState.Finished
        if (child == null) return
        val frame = view.contentWindow ?: return
        observedDocument = child
        observeDocument(view, child, frame)
        refresh(view, child, frame)
        val status = catchingNull { frameResponseStatus(frame) } ?: 0
        if (status in 400 .. 599) stateError = WebViewError(status.toLong(), "HTTP $status")
    }

    private fun observeDocument(view: HTMLIFrameElement, child: Document, frame: Window) {
        val refreshListener = webFrameListener {
            onView(view) { refresh(it, child, frame) }
        }
        val navigateListener = webFrameNavigateListener {
            onView(view) {
                if (it.contentDocument === child) {
                    stateLoadingState = WebViewLoadingState.Loading(0f)
                    stateError = null
                }
            }
        }
        val failureListener = webFrameListener {
            onView(view) {
                if (it.contentDocument === child) stateLoadingState = WebViewLoadingState.Finished
            }
        }
        val navigation = frameNavigation(frame)
        frame.addEventListener("hashchange", refreshListener)
        frame.addEventListener("popstate", refreshListener)
        navigation?.addEventListener("currententrychange", refreshListener)
        navigation?.addEventListener("navigate", navigateListener)
        navigation?.addEventListener("navigateerror", failureListener)
        val observer = MutationObserver { _, _ -> onView(view) { refresh(it, child, frame) } }
        child.head?.let { observeFrameHead(observer, it) }

        unregisterDocument = {
            observer.disconnect()
            navigation?.removeEventListener("currententrychange", refreshListener)
            navigation?.removeEventListener("navigate", navigateListener)
            navigation?.removeEventListener("navigateerror", failureListener)
            // WindowProxy 可能已经指向跨源页面。
            catching {
                if (frame.document === child) {
                    frame.removeEventListener("hashchange", refreshListener)
                    frame.removeEventListener("popstate", refreshListener)
                }
            }
        }
    }

    private fun refresh(view: HTMLIFrameElement, child: Document, frame: Window) {
        if (observedDocument !== child || view.contentDocument !== child) return
        if (child.URL != "about:blank" || currentUrl.isNotBlank()) currentUrl = child.URL
        stateTitle = child.title
        val navigation = frameNavigation(frame)
        stateCanGoBack = navigation != null && frameCanTraverse(navigation, back = true)
        stateCanGoForward = navigation != null && frameCanTraverse(navigation, back = false)
        refreshFavicon(view, child)
    }

    private fun refreshFavicon(view: HTMLIFrameElement, child: Document) {
        val href = child.querySelector("link[rel~='icon' i]")?.getAttribute("href")?.takeIf { it.isNotBlank() }
        val url = href?.let { catchingNull { URL(it, child.baseURI) } }?.takeIf { it.origin == window.location.origin && (it.protocol == "http:" || it.protocol == "https:") }?.href
        if (url == faviconUrl) return
        faviconJob?.cancel()
        faviconUrl = url
        stateIcon = null
        if (url == null) return

        launch(view) {
            if (observedDocument !== child || faviconUrl != url) return@launch
            faviconJob = coroutineContext.job
            val image = catchingNull {
                val response = fetchFrameIcon(url).await()
                if (!response.ok) return@catchingNull null
                val limit = 256 * 1024
                val length = response.headers.get("Content-Length")?.toIntOrNull()
                if (length != null && length > limit) return@catchingNull null
                val data = response.arrayBuffer().await()
                if (data.byteLength > limit) return@catchingNull null
                ImageBitmap.decode(data.asByteArray)?.let(::BitmapPainter)
            }
            if (observedDocument === child && view.contentDocument === child && faviconUrl == url) stateIcon = image
        }
    }

    private fun traverse(back: Boolean) = withView { view ->
        val child = observedDocument ?: return@withView
        if (view.contentDocument !== child) return@withView
        val frame = view.contentWindow ?: return@withView
        val navigation = frameNavigation(frame) ?: return@withView
        if (!frameCanTraverse(navigation, back)) return@withView
        launch(view) {
            if (view.contentDocument !== child) return@launch
            catchingError { frameTraverse(navigation, back).await() }?.let { failure ->
                if (view.contentDocument === child) {
                    stateError = WebViewError(0L, failure.message ?: "Navigation failed")
                    stateLoadingState = WebViewLoadingState.Finished
                }
            }
        }
    }

    private fun clearDocument() {
        unregisterDocument?.invoke()
        unregisterDocument = null
        observedDocument = null
        faviconJob?.cancel()
        faviconJob = null
        faviconUrl = null
        stateTitle = ""
        stateIcon = null
        stateCanGoBack = false
        stateCanGoForward = false
    }
}
