@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compose.ui

import org.w3c.dom.MutationObserver
import org.w3c.dom.Node
import org.w3c.dom.Window
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.EventTarget
import org.w3c.fetch.Response
import kotlin.js.Promise

internal fun webFrameListener(callback: () -> Unit): EventListener = js("({ handleEvent: function() { callback(); } })")
internal fun webFrameNavigateListener(callback: () -> Unit): EventListener = js("({ handleEvent: function(event) { if (!event.destination.sameDocument) callback(); } })")
internal fun observeFrameHead(observer: MutationObserver, head: Node): Unit = js("{ observer.observe(head, { childList: true, subtree: true, characterData: true, attributes: true }); }")
@Suppress("RedundantNullableReturnType")
internal fun frameNavigation(frame: Window): EventTarget? = js("frame.navigation || null")
internal fun frameCanTraverse(navigation: EventTarget, back: Boolean): Boolean = js("back ? navigation.canGoBack : navigation.canGoForward")

internal fun frameTraverse(navigation: EventTarget, back: Boolean): Promise<JsAny?> = js("""
    (function() {
        const traversal = back ? navigation.back() : navigation.forward();
        traversal.committed.catch(function() {});
        return traversal.finished;
    })()
""")

internal fun frameResponseStatus(frame: Window): Int = js("""
    (function() {
        const entry = frame.performance.getEntriesByType('navigation')[0];
        return entry && entry.responseStatus || 0;
    })()
""")

internal fun frameEvaluate(frame: Window, script: String): Unit = js("{ frame.eval(script); }")

internal fun fetchFrameIcon(url: String): Promise<Response> = js("fetch(url, { mode: 'same-origin' })")
