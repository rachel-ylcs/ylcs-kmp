package androidx.compose.ui.window

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.viewinterop.NativeInteropContainer
import androidx.compose.ui.viewinterop.NativeInteropView
import org.jetbrains.skiko.window.NativeWindowScope

@ExperimentalComposeUiApi
class NativeWindowContext internal constructor(val window: NativeWindowScope, private val hitTest: (Offset) -> Any?, scheduleUpdate: (() -> Unit) -> Unit) {
    internal val interopContainer = NativeInteropContainer(this, scheduleUpdate)
    private var closed = false

    fun hitTestNativeView(x: Float, y: Float): NativeInteropView? {
        if (closed || !x.isFinite() || !y.isFinite()) return null
        val view = hitTest(Offset(x, y)) as? NativeInteropView ?: return null
        return view.takeIf { interopContainer.containsView(it) }
    }

    internal fun hasNativeKeyboardFocus(): Boolean = !closed && interopContainer.hasNativeKeyboardFocus()

    internal fun updateNativeFocus() {
        if (!closed) interopContainer.hasNativeKeyboardFocus()
    }

    internal fun updateViewOrder() {
        if (!closed) interopContainer.updateOrder()
    }

    internal fun dispose() {
        closed = true
        interopContainer.dispose()
    }
}
