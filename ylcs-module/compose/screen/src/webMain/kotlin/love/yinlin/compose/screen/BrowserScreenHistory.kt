@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compose.screen

import kotlinx.browser.window
import love.yinlin.extension.catching
import love.yinlin.extension.cleaning
import org.w3c.dom.events.EventListener

@Suppress("RedundantNullableReturnType")
private fun currentHistoryMark(): String? = js("window.history.state && window.history.state.rachelScreenNavigation || null")

private fun writeHistoryMark(mark: String, push: Boolean): Unit = js("""
    {
        const state = Object.assign({}, window.history.state, { rachelScreenNavigation: mark });
        if (push) window.history.pushState(state, '');
        else window.history.replaceState(state, '');
    }
""")

private fun browserPopListener(callback: () -> Unit): EventListener = js("({ handleEvent: function () { callback(); } })")

@PublishedApi
internal class BrowserScreenHistory(private val manager: ScreenManager) {
    private val id = currentHistoryMark()?.substringBeforeLast(':') ?: ScreenID().toString()
    private val anchor = "$id:anchor"
    private val entry = "$id:back"
    private var mark = currentHistoryMark()
    private var returningToAnchor = false
    private val listener = browserPopListener(::onHistoryChanged)

    private val canPop: Boolean get() = !manager.isClosed && manager.backStack.size > 1

    fun attach() {
        window.addEventListener("popstate", listener)
        synchronize()
    }

    fun detach() {
        window.removeEventListener("popstate", listener)
        if (currentHistoryMark() == entry && !returningToAnchor) window.history.back()
    }

    fun synchronize() {
        if (returningToAnchor) return

        mark = currentHistoryMark()

        catching {
            if (canPop && mark != entry) {
                if (mark != anchor) {
                    writeHistoryMark(anchor, push = false)
                    mark = anchor
                }
                writeHistoryMark(entry, push = true)
                mark = entry
            }
            else if (!canPop && mark == entry) {
                window.history.back()
                returningToAnchor = true
            }
        }
    }

    private fun onHistoryChanged() {
        val previous = mark
        mark = currentHistoryMark()

        val returning = returningToAnchor
        returningToAnchor = false

        cleaning({
            if (!returning && previous == entry && mark == anchor && canPop) manager.pop()
        }, ::synchronize)
    }
}
