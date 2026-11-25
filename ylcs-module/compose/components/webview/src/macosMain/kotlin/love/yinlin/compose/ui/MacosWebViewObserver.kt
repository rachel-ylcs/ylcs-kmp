@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import kotlinx.cinterop.*
import platform.WebKit.WKWebView
import platform.WebKit.interop.rachel_webview_observe
import platform.WebKit.interop.rachel_webview_unobserve

internal class MacosWebViewObserver(view: WKWebView, update: (WKWebView) -> Unit) {
    private var observation: COpaquePointer?

    init {
        val context = StableRef.create { update(view) }
        val changed = staticCFunction { data: COpaquePointer? -> data?.asStableRef<() -> Unit>()?.get()?.invoke() ?: Unit }
        val release = staticCFunction { data: COpaquePointer? -> data?.asStableRef<() -> Unit>()?.dispose() ?: Unit }
        observation = rachel_webview_observe(interpretCPointer<COpaque>(view.objcPtr()), changed, release, context.asCPointer())
    }

    fun close() {
        val handle = observation ?: return
        observation = null
        rachel_webview_unobserve(handle)
    }
}
