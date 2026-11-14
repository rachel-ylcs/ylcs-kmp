package love.yinlin.compose.ui.layout

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import love.yinlin.extension.cleaning
import kotlin.math.abs

internal fun DrawScope.drawDividerTrack(
    horizontal: Boolean,
    position: Float,
    color: Color,
    surfaceColor: Color,
    thickness: Float
) {
    if (horizontal) {
        drawLine(surfaceColor, Offset(position, 0f), Offset(position, size.height), thickness + 3f)
        drawLine(color, Offset(position, 0f), Offset(position, size.height), thickness)
    }
    else {
        drawLine(surfaceColor, Offset(0f, position), Offset(size.width, position), thickness + 3f)
        drawLine(color, Offset(0f, position), Offset(size.width, position), thickness)
    }
}

internal fun DrawScope.drawDirectionalArrows(
    horizontal: Boolean,
    center: Offset,
    diameter: Float,
    color: Color
) {
    if (diameter < 8f) return

    val arrowSize = diameter * 0.2f
    val arrowOffset = diameter * 0.18f
    val strokeWidth = diameter * 0.08f

    if (horizontal) {
        val leftX = center.x - arrowOffset
        val rightX = center.x + arrowOffset
        drawLine(color, Offset(leftX + arrowSize, center.y - arrowSize), Offset(leftX, center.y), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(leftX, center.y), Offset(leftX + arrowSize, center.y + arrowSize), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(rightX - arrowSize, center.y - arrowSize), Offset(rightX, center.y), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(rightX, center.y), Offset(rightX - arrowSize, center.y + arrowSize), strokeWidth, cap = StrokeCap.Round)
    }
    else {
        val topY = center.y - arrowOffset
        val bottomY = center.y + arrowOffset
        drawLine(color, Offset(center.x - arrowSize, topY + arrowSize), Offset(center.x, topY), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(center.x, topY), Offset(center.x + arrowSize, topY + arrowSize), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(center.x - arrowSize, bottomY - arrowSize), Offset(center.x, bottomY), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(center.x, bottomY), Offset(center.x + arrowSize, bottomY - arrowSize), strokeWidth, cap = StrokeCap.Round)
    }
}

internal fun constrainedDividerRatio(ratio: Float, axisExtent: Float, gripExtent: Float): Float {
    if (axisExtent <= 0f) return 0.5f
    val minRatio = (gripExtent / (axisExtent * 2f)).coerceIn(0f, 0.5f)
    return ratio.coerceIn(minRatio, 1f - minRatio)
}

private fun dividerCenter(
    axisExtent: Int,
    gripExtent: Int,
    ratio: Float,
    horizontal: Boolean,
    layoutDirection: LayoutDirection
): Float {
    val boundedRatio = constrainedDividerRatio(ratio, axisExtent.toFloat(), gripExtent.toFloat())
    val visualRatio = if (horizontal && layoutDirection == LayoutDirection.Rtl) 1f - boundedRatio else boundedRatio
    return axisExtent * visualRatio
}

internal fun Modifier.dividerGestures(
    horizontal: Boolean,
    gripSize: Dp,
    ratio: () -> Float,
    onRatioChange: (Float) -> Unit,
    onHoverChange: (Boolean) -> Unit,
    onDraggingChange: (Boolean) -> Unit,
    hitTargetSize: Dp,
    layoutDirection: LayoutDirection
): Modifier = this.pointerInput(horizontal, gripSize, hitTargetSize, layoutDirection) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            when (event.type) {
                PointerEventType.Exit -> onHoverChange(false)
                PointerEventType.Enter, PointerEventType.Move -> {
                    val change = event.changes.firstOrNull() ?: continue

                    val axisExtent = if (horizontal) size.width else size.height
                    val inside = change.position.x >= 0f && change.position.x < size.width && change.position.y >= 0f && change.position.y < size.height
                    val gripExtent = gripSize.roundToPx()
                    val center = dividerCenter(axisExtent, gripExtent, ratio(), horizontal, layoutDirection)
                    val hitExtent = maxOf(hitTargetSize.roundToPx(), gripExtent)
                    val pointerAxis = if (horizontal) change.position.x else change.position.y

                    onHoverChange(change.type == PointerType.Mouse && inside && axisExtent > 0 && abs(pointerAxis - center) <= hitExtent / 2f)
                }
            }
        }
    }
}.pointerInput(horizontal, gripSize, hitTargetSize, layoutDirection) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val axisExtent = if (horizontal) size.width else size.height
        if (axisExtent <= 0) return@awaitEachGesture

        val gripExtent = gripSize.roundToPx()
        val center = dividerCenter(axisExtent, gripExtent, ratio(), horizontal, layoutDirection)
        val hitExtent = maxOf(hitTargetSize.roundToPx(), gripExtent)
        val downAxis = if (horizontal) down.position.x else down.position.y
        if (abs(downAxis - center) > hitExtent / 2f) return@awaitEachGesture

        val grabOffset = downAxis - center
        onDraggingChange(true)

        cleaning({
            down.consume()
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                val currentExtent = if (horizontal) size.width else size.height
                if (currentExtent > 0) {
                    val pointerAxis = if (horizontal) change.position.x else change.position.y
                    val visualRatio = ((pointerAxis - grabOffset) / currentExtent).coerceIn(0f, 1f)
                    val logicalRatio = if (horizontal && layoutDirection == LayoutDirection.Rtl) 1f - visualRatio else visualRatio
                    val nextRatio = constrainedDividerRatio(logicalRatio, currentExtent.toFloat(), gripSize.toPx())
                    if (nextRatio != ratio()) {
                        onRatioChange(nextRatio)
                        change.consume()
                    }
                }

                if (!change.pressed) break
            }
        }) {
            onDraggingChange(false)
        }
    }
}
