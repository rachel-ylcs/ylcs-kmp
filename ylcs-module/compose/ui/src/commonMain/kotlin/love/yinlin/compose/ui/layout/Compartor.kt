package love.yinlin.compose.ui.layout

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.ui.node.pointerIcon
import kotlin.math.abs

private const val COMPARATOR_MIN_RATIO = 0.05f
private const val COMPARATOR_MAX_RATIO = 0.95f

/**
 * 将内容叠放为左右两部分，通过拖动中间的比较线调整两侧的可见范围。
 *
 * 两个内容都会在比较器的完整尺寸下测量，分别只绘制比较线一侧的区域。
 * 未指定尺寸且遇到无限约束时，使用 Theme.size.cell1 作为有限尺寸。
 *
 * @param modifier 比较器尺寸与布局修饰符
 * @param left 左侧内容；RTL 布局下位于逻辑末尾
 * @param right 右侧内容；RTL 布局下位于逻辑起始
 */
@Composable
fun HorizontalComparator(
    modifier: Modifier = Modifier,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    ComparatorLayout(
        horizontal = true,
        modifier = modifier,
        first = left,
        second = right,
    )
}

/**
 * 将内容叠放为上下两部分，通过拖动中间的比较线调整上下两侧的可见范围。
 *
 * 两个内容都会在比较器的完整尺寸下测量，分别只绘制比较线一侧的区域。
 * 未指定尺寸且遇到无限约束时，使用 Theme.size.cell1 作为有限尺寸。
 *
 * @param modifier 比较器尺寸与布局修饰符
 * @param top 上方内容
 * @param bottom 下方内容
 */
@Composable
fun VerticalComparator(
    modifier: Modifier = Modifier,
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
) {
    ComparatorLayout(
        horizontal = false,
        modifier = modifier,
        first = top,
        second = bottom,
    )
}

@Composable
private fun ComparatorLayout(
    horizontal: Boolean,
    modifier: Modifier,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
) {
    var splitRatio by rememberValueState(0.5f, horizontal)
    var hovered by rememberFalse(horizontal)
    var dragging by rememberFalse(horizontal)
    val fallbackSize = Theme.size.cell1
    val hitTargetSize = Theme.size.box1
    val layoutDirection = LocalLayoutDirection.current
    val layoutModifier = if (horizontal) modifier.fillMaxWidth() else modifier.fillMaxHeight()

    Layout(
        modifier = layoutModifier
            .pointerIcon(if (hovered) PointerIcon.Hand else PointerIcon.Default)
            .pointerInput(horizontal, hitTargetSize, layoutDirection) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.type == PointerType.Mouse) {
                            when (event.type) {
                                PointerEventType.Enter, PointerEventType.Move -> {
                                    val axisExtent = if (horizontal) size.width else size.height
                                    val splitPosition = if (horizontal && layoutDirection == LayoutDirection.Rtl) {
                                        axisExtent * (1f - splitRatio)
                                    }
                                    else axisExtent * splitRatio
                                    val pointerPosition = if (horizontal) change.position.x else change.position.y
                                    hovered = axisExtent > 0 && abs(pointerPosition - splitPosition) <= hitTargetSize.roundToPx() / 2f
                                }
                                PointerEventType.Exit -> hovered = false
                            }
                        }
                    }
                }
            }
            .pointerInput(horizontal, hitTargetSize, layoutDirection) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val axisExtent = if (horizontal) size.width else size.height
                    if (axisExtent <= 0) return@awaitEachGesture

                    val splitPosition = if (horizontal && layoutDirection == LayoutDirection.Rtl) {
                        axisExtent * (1f - splitRatio)
                    }
                    else axisExtent * splitRatio
                    val downPosition = if (horizontal) down.position.x else down.position.y
                    if (abs(downPosition - splitPosition) > hitTargetSize.roundToPx() / 2f) return@awaitEachGesture

                    dragging = true
                    try {
                        down.consume()
                        var previousPosition = downPosition

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val currentPosition = if (horizontal) change.position.x else change.position.y
                            val delta = currentPosition - previousPosition
                            val logicalDelta = if (horizontal && layoutDirection == LayoutDirection.Rtl) -delta else delta
                            if (logicalDelta != 0f) {
                                splitRatio = (splitRatio + logicalDelta / axisExtent)
                                    .coerceIn(COMPARATOR_MIN_RATIO, COMPARATOR_MAX_RATIO)
                                change.consume()
                            }

                            previousPosition = currentPosition
                            if (!change.pressed) break
                        }
                    } finally {
                        dragging = false
                    }
                }
            },
        content = {
            ComparatorPane(horizontal, true, splitRatio, layoutDirection, first)
            ComparatorPane(horizontal, false, splitRatio, layoutDirection, second)
            ComparatorHandle(horizontal, splitRatio, layoutDirection, hovered, dragging)
        },
    ) { measurables, constraints ->
        val fallbackExtent = fallbackSize.roundToPx().coerceAtLeast(0)
        val width = constraints.constrainWidth(
            if (constraints.hasBoundedWidth) constraints.maxWidth else maxOf(fallbackExtent, constraints.minWidth)
        )
        val height = constraints.constrainHeight(
            if (constraints.hasBoundedHeight) constraints.maxHeight else maxOf(fallbackExtent, constraints.minHeight)
        )
        val childConstraints = Constraints.fixed(width, height)
        val firstPlaceable = measurables[0].measure(childConstraints)
        val secondPlaceable = measurables[1].measure(childConstraints)
        val handlePlaceable = measurables[2].measure(childConstraints)

        layout(width, height) {
            firstPlaceable.place(0, 0)
            secondPlaceable.place(0, 0)
            handlePlaceable.place(0, 0)
        }
    }
}

@Composable
private fun ComparatorPane(
    horizontal: Boolean,
    firstPane: Boolean,
    splitRatio: Float,
    layoutDirection: LayoutDirection,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().drawWithContent {
            if (horizontal) {
                val splitPosition = size.width * if (layoutDirection == LayoutDirection.Rtl) 1f - splitRatio else splitRatio
                val showLeftSide = firstPane == (layoutDirection == LayoutDirection.Ltr)
                if (showLeftSide) clipRect(right = splitPosition) { this@drawWithContent.drawContent() }
                else clipRect(left = splitPosition) { this@drawWithContent.drawContent() }
            }
            else {
                val splitPosition = size.height * splitRatio
                if (firstPane) clipRect(bottom = splitPosition) { this@drawWithContent.drawContent() }
                else clipRect(top = splitPosition) { this@drawWithContent.drawContent() }
            }
        },
    ) {
        content()
    }
}

@Composable
private fun ComparatorHandle(
    horizontal: Boolean,
    splitRatio: Float,
    layoutDirection: LayoutDirection,
    hovered: Boolean,
    dragging: Boolean,
) {
    val inactiveColor = Theme.color.outline.copy(alpha = 0.4f)
    val activeColor = Theme.color.primary
    val indicatorColor = Theme.color.primaryContainer
    val lineThickness = Theme.border.v7
    val indicatorThickness = Theme.size.box3
    val indicatorLength = Theme.size.input9

    Box(
        modifier = Modifier.fillMaxSize().drawWithContent {
            val splitPosition = if (horizontal && layoutDirection == LayoutDirection.Rtl) {
                size.width * (1f - splitRatio)
            }
            else if (horizontal) size.width * splitRatio
            else size.height * splitRatio

            val lineColor = if (hovered || dragging) activeColor else inactiveColor
            val strokeWidth = lineThickness.toPx().coerceAtLeast(0f)
            if (horizontal) {
                drawLine(lineColor, Offset(splitPosition, 0f), Offset(splitPosition, size.height), strokeWidth)
                if (hovered || dragging) {
                    val width = indicatorThickness.toPx().coerceAtLeast(0f).coerceAtMost(size.width)
                    val height = indicatorLength.toPx().coerceAtLeast(0f).coerceAtMost(size.height)
                    if (width > 0f && height > 0f) {
                        drawRoundRect(
                            color = if (dragging) activeColor else indicatorColor,
                            topLeft = Offset(splitPosition - width / 2f, (size.height - height) / 2f),
                            size = Size(width, height),
                            cornerRadius = CornerRadius(width / 2f),
                        )
                    }
                }
            }
            else {
                drawLine(lineColor, Offset(0f, splitPosition), Offset(size.width, splitPosition), strokeWidth)
                if (hovered || dragging) {
                    val width = indicatorLength.toPx().coerceAtLeast(0f).coerceAtMost(size.width)
                    val height = indicatorThickness.toPx().coerceAtLeast(0f).coerceAtMost(size.height)
                    if (width > 0f && height > 0f) {
                        drawRoundRect(
                            color = if (dragging) activeColor else indicatorColor,
                            topLeft = Offset((size.width - width) / 2f, splitPosition - height / 2f),
                            size = Size(width, height),
                            cornerRadius = CornerRadius(height / 2f),
                        )
                    }
                }
            }
        },
    )
}
