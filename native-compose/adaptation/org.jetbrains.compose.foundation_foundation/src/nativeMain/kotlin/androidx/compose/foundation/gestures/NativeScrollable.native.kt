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

internal actual fun CompositionLocalConsumerModifierNode.platformScrollConfig(): ScrollConfig = NativeScrollConfig

/** SDL reports wheel units, already normalized for flipped devices by the window host. */
private object NativeScrollConfig : ScrollConfig {
    override fun isPreciseWheelScroll(event: PointerEvent): Boolean = event.changes.any {
        abs(it.scrollDelta.x - round(it.scrollDelta.x)) > 0.001f || abs(it.scrollDelta.y - round(it.scrollDelta.y)) > 0.001f
    }

    override fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset {
        val delta = event.changes.fold(Offset.Zero) { total, change -> total + change.scrollDelta }
        val step = if (DesktopPlatform.Current == DesktopPlatform.MacOS) 10.dp else 48.dp
        return -delta * step.toPx()
    }
}
