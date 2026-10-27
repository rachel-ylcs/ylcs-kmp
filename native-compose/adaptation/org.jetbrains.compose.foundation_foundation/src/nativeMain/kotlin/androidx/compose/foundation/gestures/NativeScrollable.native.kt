package androidx.compose.foundation.gestures

import androidx.compose.foundation.DesktopPlatform
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sqrt

internal actual fun CompositionLocalConsumerModifierNode.platformScrollConfig(): ScrollConfig = NativeScrollConfig

/** SDL reports wheel units, already normalized for flipped devices by the window host. */
private object NativeScrollConfig : ScrollConfig {
    override fun isPreciseWheelScroll(event: PointerEvent): Boolean = DesktopPlatform.Current != DesktopPlatform.Windows && event.changes.any {
        abs(it.scrollDelta.x - round(it.scrollDelta.x)) > 0.001f || abs(it.scrollDelta.y - round(it.scrollDelta.y)) > 0.001f
    }

    override fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset {
        val delta = event.changes.fold(Offset.Zero) { total, change -> total + change.scrollDelta }
        val lines = nativeScrollLineCount()
        // Match DesktopScrollable.desktop.kt; SDL wheel events contain notches, not pixels or lines.
        if (lines == -1) return Offset(-delta.x * bounds.width, -delta.y * bounds.height)
        val step = when (DesktopPlatform.Current) {
            DesktopPlatform.Windows, DesktopPlatform.Unknown -> Offset(bounds.width / 20f, bounds.height / 20f)
            DesktopPlatform.Linux -> Offset(sqrt(bounds.width.toFloat()), sqrt(bounds.height.toFloat()))
            DesktopPlatform.MacOS -> Offset(10.dp.toPx(), 10.dp.toPx())
        }
        return Offset(-delta.x * step.x * lines, -delta.y * step.y * lines)
    }
}

/** Number of lines per wheel notch; -1 requests page scrolling and 0 disables scrolling. */
internal expect fun nativeScrollLineCount(): Int
