package androidx.compose.ui.platform

import org.jetbrains.skiko.window.NativeWindowCursor
import org.jetbrains.skiko.window.NativeWindowScope

/** Headless host controls: scene tests use real Compose/Skia without creating an SDL window. */
internal class TestNativeWindowScope(override val displayScale: Float = 1f, override val pixelDensity: Float = 1f) : NativeWindowScope {
    override val pixelWidth = 96
    override val pixelHeight = 64
    var invalidations = 0
    var closes = 0
    var inputStarts = 0
    var inputStops = 0
    var inputArea: List<Int> = []
    override fun invalidate() { invalidations++ }
    override fun close() { closes++ }
    override fun setSize(width: Int, height: Int) = Unit
    override fun setCursor(cursor: NativeWindowCursor) = Unit
    override fun startTextInput() { inputStarts++ }
    override fun stopTextInput() { inputStops++ }
    override fun setTextInputArea(x: Int, y: Int, width: Int, height: Int) { inputArea = [x, y, width, height] }
}
