package love.yinlin.compose.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.viewinterop.NativeInteropView

@OptIn(ExperimentalComposeUiApi::class)
internal class NativePlatformViewMount<V : Any, P : Any>(private val slot: PlatformViewSlot<V, P>) : NativeInteropView {
    private var container: NativePlatformViewContainer<V>? = null
    private var onNativeFocus: () -> Unit = {}
    private var nativeFocused = false

    override val nativeHandle: Long get() = container?.nativeHandle ?: 0L
    override val hasFocus: Boolean get() {
        val focused = container?.hasFocus == true
        if (focused && !nativeFocused) onNativeFocus()
        nativeFocused = focused
        return focused
    }

    fun attach(container: NativePlatformViewContainer<V>, parameters: P, onNativeFocus: () -> Unit) {
        this.container = container
        this.onNativeFocus = onNativeFocus
        try {
            slot.onForgotten = container::hide
            slot.create(parameters, container::create)
            slot.update(parameters)
        }
        catch (failure: Throwable) {
            try { close() }
            catch (cleanup: Throwable) { if (cleanup !== failure) failure.addSuppressed(cleanup) }
            throw failure
        }
    }

    override fun place(bounds: Rect, visibleBounds: Rect) {
        val container = container ?: return
        container.place(bounds, visibleBounds)
    }

    override fun bringToFront() { container?.bringToFront() }
    override fun hide() { container?.hide() }

    override fun close() {
        val previous = container
        container = null
        onNativeFocus = { }
        platformViewCleanup(
            slot::finish,
            { previous?.close() }
        )
    }
}
