package androidx.compose.ui.window

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.platform.nativeUiDispatcher
import org.jetbrains.skiko.window.NativeWindowScope

/** Application-owned composition, driven even when the main window is hidden or minimized. */
internal class NativeApplicationComposition(private val content: @Composable () -> Unit) : AutoCloseable {
    private val dispatcher = nativeUiDispatcher().claim()
    private var wake: () -> Unit = {}
    private val recomposer = FrameRecomposer(dispatcher.context) { wake() }
    private val composition = Composition(object : AbstractApplier<Unit>(Unit) {
        override fun onClear() = Unit
        override fun insertTopDown(index: Int, instance: Unit) = error("Application content must create windows rather than UI nodes")
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun remove(index: Int, count: Int) = Unit
    }, recomposer.compositionContext)
    private var closed = false

    fun attach(window: NativeWindowScope) {
        wake = window::invalidate
        dispatcher.wake = window::invalidate
        composition.setContent(content)
    }

    fun frame(nanoTime: Long) {
        dispatcher.runCurrent()
        recomposer.performFrame(nanoTime)
        dispatcher.runCurrent()
    }

    override fun close() {
        if (closed) return
        closed = true
        try { composition.dispose() }
        finally {
            try { recomposer.close() }
            finally {
                try { dispatcher.close() }
                finally { wake = {} }
            }
        }
    }
}
