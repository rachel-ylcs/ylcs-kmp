@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compose.window

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Stable
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import love.yinlin.extension.catching
import love.yinlin.extension.cleaning
import love.yinlin.foundation.PlatformContextProvider
import org.w3c.dom.Element
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener

private fun fullscreenListener(callback: (Event) -> Unit): EventListener = js("({ handleEvent: callback })")

@Stable
actual class OrientationController {
    private val scope = MainScope()
    private var ownedFullscreen: Element? = null
    private var changing = false
    private var restorePending = false
    private val onFullscreenChange = fullscreenListener { updateFullscreen() }

    actual fun getOrientation(context: PlatformContextProvider): Orientation = if (document.fullscreenElement != null) Orientation.Horizontal else Orientation.Vertical

    actual fun setOrientation(context: PlatformContextProvider, orientation: Orientation) {
        if (orientation == Orientation.Vertical) {
            if (changing) restorePending = true
            else if (document.fullscreenElement != null) change { exitFullscreen() }
            return
        }

        val root = document.documentElement ?: return
        if (changing || document.fullscreenElement != null || !document.fullscreenEnabled) return

        change {
            catching {
                root.requestFullscreen().await()
                if (document.fullscreenElement === root) {
                    ownedFullscreen = root
                    document.addEventListener("fullscreenchange", onFullscreenChange)
                }
            }
        }
    }

    actual fun rotate(context: PlatformContextProvider) {
        if (changing) return

        val target = if (getOrientation(context) == Orientation.Horizontal) Orientation.Vertical else Orientation.Horizontal
        setOrientation(context, target)
    }

    actual fun store(context: PlatformContextProvider) {
        if (changing) restorePending = true
        else change { restore() }
    }

    private fun change(block: suspend () -> Unit) {
        if (changing) return

        changing = true
        // 在用户点击回调内发出 requestFullscreen，保留瞬时用户激活
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            cleaning({
                block()
                if (restorePending) restore()
            }) {
                restorePending = false
                changing = false
            }
        }
    }

    private suspend fun exitFullscreen() {
        catching { document.exitFullscreen().await() }
        updateFullscreen()
    }

    private suspend fun restore() {
        updateFullscreen()
        if (ownedFullscreen != null) exitFullscreen()
    }

    private fun updateFullscreen() {
        val fullscreen = ownedFullscreen ?: return

        if (document.fullscreenElement === fullscreen) return

        ownedFullscreen = null
        document.removeEventListener("fullscreenchange", onFullscreenChange)
    }
}
