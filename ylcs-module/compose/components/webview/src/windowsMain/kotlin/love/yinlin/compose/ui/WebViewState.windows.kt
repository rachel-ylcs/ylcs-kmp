@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import kotlinx.cinterop.*
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.webview.internal.*

@Stable
actual class WebViewState actual constructor(initUrl: String) : PlatformViewState<COpaquePointer, WebViewParameters>() {
    private var stateUrl by mutableStateOf(initUrl)
    private var stateLoadingState: WebViewLoadingState by mutableRefStateOf(WebViewLoadingState.Initializing)
    private var stateTitle by mutableStateOf("")
    private var stateCanGoBack by mutableStateOf(false)
    private var stateCanGoForward by mutableStateOf(false)
    private var stateError: WebViewError? by mutableRefStateOf(null)
    private var requestedUrl = initUrl
    private var requestVersion = 0L
    private var appliedVersion = -1L
    internal var available by mutableStateOf(true)
        private set

    actual var url: String
        get() = stateUrl
        set(value) {
            requestedUrl = value
            stateUrl = value
            requestVersion++
            withView { navigate(it, value) }
        }
    actual val loadingState: WebViewLoadingState get() = stateLoadingState
    actual val title: String get() = stateTitle
    actual val icon: BitmapPainter? = null
    actual val canGoBack: Boolean get() = stateCanGoBack
    actual val canGoForward: Boolean get() = stateCanGoForward
    actual val error: WebViewError? get() = stateError

    actual fun goBack() = withView { rachel_webview_back(it) }
    actual fun goForward() = withView { rachel_webview_forward(it) }
    actual fun evaluateJavaScript(script: String) = withView { rachel_webview_script(it, script) }

    override fun factory(environment: PlatformViewEnvironment): COpaquePointer {
        val listener: (COpaquePointer, Int, Long, String?) -> Unit = { view, event, value, text ->
            withView { active -> if (active == view) receive(event, value, text) }
        }
        val context = StableRef.create(listener)
        val callback = staticCFunction { data: COpaquePointer?, view: COpaquePointer?, event: Int, value: Long, text: CPointer<ByteVar>? ->
            if (data != null && view != null) data.asStableRef<(COpaquePointer, Int, Long, String?) -> Unit>().get().invoke(view, event, value, text?.toKString())
        }
        val release = staticCFunction { data: COpaquePointer? ->
            data?.asStableRef<(COpaquePointer, Int, Long, String?) -> Unit>()?.dispose() ?: Unit
        }
        return checkNotNull(rachel_webview_create(environment.parentWindow, callback, release, context.asCPointer())) { "Cannot create the WebView2 host window" }
    }

    override fun attach(view: COpaquePointer) {
        appliedVersion = -1L
        stateLoadingState = WebViewLoadingState.Initializing
        stateCanGoBack = false
        stateCanGoForward = false
        rachel_webview_start(view)
    }

    override fun synchronize(view: COpaquePointer, previous: WebViewParameters?, next: WebViewParameters) {
        if (previous?.config?.enableJavaScript != next.config.enableJavaScript) rachel_webview_configure(view, next.config.enableJavaScript)
        if (appliedVersion != requestVersion) navigate(view, requestedUrl)
    }

    private fun navigate(view: COpaquePointer, target: String) {
        appliedVersion = requestVersion
        rachel_webview_load(view, target)
    }

    private fun receive(event: Int, value: Long, text: String?) {
        when (event.toUInt()) {
            RACHEL_WEBVIEW_READY -> stateLoadingState = WebViewLoadingState.Finished
            RACHEL_WEBVIEW_UNAVAILABLE -> {
                available = false
                stateLoadingState = WebViewLoadingState.Finished
                stateError = WebViewError(value, "WebView2 is unavailable")
            }
            RACHEL_WEBVIEW_STARTED -> {
                stateError = null
                stateLoadingState = WebViewLoadingState.Loading(0f)
            }
            RACHEL_WEBVIEW_CONTENT -> stateLoadingState = WebViewLoadingState.Loading(0.5f)
            RACHEL_WEBVIEW_FINISHED -> stateLoadingState = WebViewLoadingState.Finished
            RACHEL_WEBVIEW_URL -> if (!text.isNullOrEmpty()) {
                stateUrl = text
                requestedUrl = text
            }
            RACHEL_WEBVIEW_TITLE -> stateTitle = text.orEmpty()
            RACHEL_WEBVIEW_HISTORY -> {
                stateCanGoBack = value and 1L != 0L
                stateCanGoForward = value and 2L != 0L
            }
            RACHEL_WEBVIEW_ERROR -> {
                stateError = WebViewError(value, text.orEmpty())
                stateLoadingState = WebViewLoadingState.Finished
            }
        }
    }

    override fun detach(view: COpaquePointer) {
        rachel_webview_close(view)
        stateCanGoBack = false
        stateCanGoForward = false
    }
}
