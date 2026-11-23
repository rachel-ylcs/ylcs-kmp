@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compose

import org.w3c.dom.PageTransitionEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.EventTarget

private fun listener(callback: (Event) -> Unit): EventListener = js("({ handleEvent: callback })")

internal class WebPageLifecycle(
    private val target: EventTarget,
    private val onReady: () -> Unit,
    private val onClose: () -> Unit
) {
    private var ready = false
    private var closed = false
    private val loadListener = listener { start() }
    private val pageHideListener = listener { event ->
        if (event is PageTransitionEvent && !event.persisted) close()
    }

    fun bind(alreadyLoaded: Boolean) {
        target.addEventListener("pagehide", pageHideListener)
        if (alreadyLoaded) start()
        else target.addEventListener("load", loadListener)
    }

    private fun start() {
        if (ready || closed) return
        ready = true
        target.removeEventListener("load", loadListener)
        onReady()
    }

    fun close() {
        if (closed) return
        closed = true
        target.removeEventListener("load", loadListener)
        target.removeEventListener("pagehide", pageHideListener)
        onClose()
    }
}
