package androidx.compose.ui.platform

import org.jetbrains.skiko.window.NativeWindowCursor
import org.jetbrains.skiko.window.NativeWindowDropTarget
import org.jetbrains.skiko.window.NativeWindowScope
import org.jetbrains.skiko.window.NativeWindowState
import org.jetbrains.skiko.window.NativeTray

internal class TestNativeWindowScope(override val displayScale: Float = 1f, override val pixelDensity: Float = 1f) : NativeWindowScope {
    override val pixelWidth = 96
    override val pixelHeight = 64
    override val state get() = NativeWindowState("", pixelWidth, pixelHeight)
    var invalidations = 0
    var closes = 0
    var inputStarts = 0
    var inputStops = 0
    var inputArea: List<Int> = []
    override fun invalidate() { invalidations++ }
    override fun close() { closes++ }
    override fun setSize(width: Int, height: Int) = Unit
    override fun setTitle(title: String) = Unit
    override fun setMinimumSize(width: Int, height: Int) = Unit
    override fun setUndecorated(undecorated: Boolean) = Unit
    override fun setIcon(width: Int, height: Int, pixels: IntArray): Boolean = true
    override fun createTray(tooltip: String): NativeTray? = null
    override fun setDragArea(hitTest: ((Float, Float) -> Boolean)?) = Unit
    override fun setVisible(visible: Boolean) = Unit
    override fun setAlwaysOnTop(alwaysOnTop: Boolean) = Unit
    override fun setMinimized(minimized: Boolean) = Unit
    override fun setMaximized(maximized: Boolean) = Unit
    override fun setCursor(cursor: NativeWindowCursor) = Unit
    override fun setDropTarget(target: NativeWindowDropTarget?) = Unit
    override fun startTextInput() { inputStarts++ }
    override fun stopTextInput() { inputStops++ }
    override fun setTextInputArea(x: Int, y: Int, width: Int, height: Int) { inputArea = [x, y, width, height] }
}
