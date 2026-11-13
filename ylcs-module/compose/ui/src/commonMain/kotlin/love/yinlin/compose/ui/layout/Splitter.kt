package love.yinlin.compose.ui.layout

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastRoundToInt
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.ui.node.pointerIcon
import kotlin.math.abs

private const val SPLITTER_MIN_RATIO = 0.05f
private const val SPLITTER_MAX_RATIO = 0.95f

/**
 * 将内容分成左右两个可调节区域。
 *
 * 两侧内容分别在自己的区域约束下测量；需要占满分区时，在内容上使用 [Modifier.fillMaxWidth]。
 * 沿分隔轴或交叉轴遇到无限约束时，以 Theme.size.cell1 作为有限测量上限。
 *
 * @param modifier 分隔器尺寸与布局修饰符
 * @param left 起始侧内容，RTL 布局下位于右侧
 * @param right 末尾侧内容，RTL 布局下位于左侧
 * @param alignment 两侧内容的垂直对齐方式
 */
@Composable
fun HorizontalSplitter(
    modifier: Modifier = Modifier,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
    alignment: Alignment.Vertical = Alignment.CenterVertically,
) {
    Splitter(
        horizontal = true,
        modifier = modifier,
        verticalAlignment = alignment,
        horizontalAlignment = Alignment.CenterHorizontally,
        first = left,
        second = right,
    )
}

/**
 * 将内容分成上下两个可调节区域。
 *
 * 上下内容分别在自己的区域约束下测量；需要占满分区时，在内容上使用 [Modifier.fillMaxHeight]。
 * 沿分隔轴或交叉轴遇到无限约束时，以 Theme.size.cell1 作为有限测量上限。
 *
 * @param modifier 分隔器尺寸与布局修饰符
 * @param top 上方内容
 * @param bottom 下方内容
 * @param alignment 上下内容的水平对齐方式
 */
@Composable
fun VerticalSplitter(
    modifier: Modifier = Modifier,
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
) {
    Splitter(
        horizontal = false,
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = alignment,
        first = top,
        second = bottom,
    )
}

@Composable
private fun Splitter(
    horizontal: Boolean,
    modifier: Modifier,
    verticalAlignment: Alignment.Vertical,
    horizontalAlignment: Alignment.Horizontal,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
) {
    var splitRatio by rememberValueState(0.5f, horizontal)
    var dragging by rememberFalse(horizontal)
    val fallbackSize = Theme.size.cell1
    val dividerSize = Theme.size.box1
    val layoutDirection = LocalLayoutDirection.current
    val layoutModifier = if (horizontal) modifier.fillMaxWidth() else modifier.fillMaxHeight()

    Layout(
        modifier = layoutModifier.pointerInput(horizontal, dividerSize, layoutDirection) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val axisExtent = if (horizontal) size.width else size.height
                val dividerExtent = dividerSize.roundToPx().coerceAtLeast(0).coerceAtMost(axisExtent)
                val availableExtent = (axisExtent - dividerExtent).coerceAtLeast(0)
                val firstExtent = (availableExtent * splitRatio.coerceIn(SPLITTER_MIN_RATIO, SPLITTER_MAX_RATIO))
                    .fastRoundToInt()
                    .coerceIn(0, availableExtent)
                val downAxis = if (horizontal) down.position.x else down.position.y
                val dividerCenter = if (horizontal && layoutDirection == LayoutDirection.Rtl) axisExtent - firstExtent - dividerExtent / 2f else firstExtent + dividerExtent / 2f

                if (abs(downAxis - dividerCenter) > dividerExtent / 2f) return@awaitEachGesture

                dragging = true
                try {
                    down.consume()
                    var previous = downAxis

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val current = if (horizontal) change.position.x else change.position.y
                        val delta = current - previous
                        val currentAxisExtent = if (horizontal) size.width else size.height
                        val currentDividerExtent = dividerSize.roundToPx().coerceAtLeast(0).coerceAtMost(currentAxisExtent)
                        val currentAvailableExtent = (currentAxisExtent - currentDividerExtent).coerceAtLeast(0)

                        if (delta != 0f && currentAvailableExtent > 0) {
                            val logicalDelta = if (horizontal && layoutDirection == LayoutDirection.Rtl) -delta else delta
                            splitRatio = (splitRatio + logicalDelta / currentAvailableExtent)
                                .coerceIn(SPLITTER_MIN_RATIO, SPLITTER_MAX_RATIO)
                            change.consume()
                        }

                        previous = current
                        if (!change.pressed) break
                    }
                } finally {
                    dragging = false
                }
            }
        },
        content = {
            Box(content = { first() })
            Box(content = { second() })
            SplitterHandle(horizontal, dragging)
        },
    ) { measurables, constraints ->
        val fallbackExtent = fallbackSize.roundToPx().coerceAtLeast(0)
        val axisExtent = when {
            horizontal && constraints.hasBoundedWidth -> constraints.maxWidth
            !horizontal && constraints.hasBoundedHeight -> constraints.maxHeight
            horizontal -> maxOf(fallbackExtent, constraints.minWidth)
            else -> maxOf(fallbackExtent, constraints.minHeight)
        }
        val crossExtent = when {
            horizontal && constraints.hasBoundedHeight -> constraints.maxHeight
            !horizontal && constraints.hasBoundedWidth -> constraints.maxWidth
            horizontal -> maxOf(fallbackExtent, constraints.minHeight)
            else -> maxOf(fallbackExtent, constraints.minWidth)
        }
        val dividerExtent = dividerSize.roundToPx().coerceAtLeast(0).coerceAtMost(axisExtent)
        val availableExtent = (axisExtent - dividerExtent).coerceAtLeast(0)
        val firstExtent = (availableExtent * splitRatio.coerceIn(SPLITTER_MIN_RATIO, SPLITTER_MAX_RATIO))
            .fastRoundToInt()
            .coerceIn(0, availableExtent)
        val secondExtent = availableExtent - firstExtent

        val firstConstraints = if (horizontal) {
            Constraints(maxWidth = firstExtent, maxHeight = crossExtent)
        }
        else {
            Constraints(maxWidth = crossExtent, maxHeight = firstExtent)
        }
        val secondConstraints = if (horizontal) {
            Constraints(maxWidth = secondExtent, maxHeight = crossExtent)
        }
        else {
            Constraints(maxWidth = crossExtent, maxHeight = secondExtent)
        }

        val firstPlaceable = measurables[0].measure(firstConstraints)
        val secondPlaceable = measurables[1].measure(secondConstraints)

        if (horizontal) {
            val width = constraints.constrainWidth(axisExtent)
            val height = constraints.constrainHeight(maxOf(firstPlaceable.height, secondPlaceable.height))
            val handlePlaceable = measurables[2].measure(Constraints.fixed(dividerExtent, height))

            layout(width, height) {
                firstPlaceable.placeRelative(0, verticalAlignment.align(firstPlaceable.height, height))
                handlePlaceable.placeRelative(firstExtent, 0)
                secondPlaceable.placeRelative(firstExtent + dividerExtent, verticalAlignment.align(secondPlaceable.height, height))
            }
        }
        else {
            val width = constraints.constrainWidth(maxOf(firstPlaceable.width, secondPlaceable.width))
            val height = constraints.constrainHeight(axisExtent)
            val handlePlaceable = measurables[2].measure(Constraints.fixed(width, dividerExtent))

            layout(width, height) {
                firstPlaceable.place(horizontalAlignment.align(firstPlaceable.width, width, layoutDirection), 0)
                handlePlaceable.place(0, firstExtent)
                secondPlaceable.place(horizontalAlignment.align(secondPlaceable.width, width, layoutDirection), firstExtent + dividerExtent)
            }
        }
    }
}

@Composable
private fun SplitterHandle(
    horizontal: Boolean,
    dragging: Boolean,
) {
    var hovered by rememberFalse(horizontal)
    val inactiveColor = Theme.color.outline.copy(alpha = 0.4f)
    val activeColor = Theme.color.primary
    val indicatorColor = Theme.color.primaryContainer
    val lineThickness = Theme.border.v7
    val indicatorThickness = Theme.size.box3
    val indicatorLength = Theme.size.input9

    Box(
        modifier = Modifier
            .pointerIcon(PointerIcon.Hand)
            .pointerInput(horizontal) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.type == PointerType.Mouse) {
                            when (event.type) {
                                PointerEventType.Enter, PointerEventType.Move -> hovered = true
                                PointerEventType.Exit -> hovered = false
                            }
                        }
                    }
                }
            }
            .drawWithContent {
                drawContent()

                val lineColor = if (hovered || dragging) activeColor else inactiveColor
                val strokeWidth = lineThickness.toPx().coerceAtLeast(0f)
                if (horizontal) {
                    drawLine(lineColor, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), strokeWidth)
                    if (hovered || dragging) {
                        val width = indicatorThickness.toPx().coerceAtLeast(0f).coerceAtMost(size.width)
                        val height = indicatorLength.toPx().coerceAtLeast(0f).coerceAtMost(size.height)
                        if (width > 0f && height > 0f) {
                            drawRoundRect(
                                color = if (dragging) activeColor else indicatorColor,
                                topLeft = Offset((size.width - width) / 2f, (size.height - height) / 2f),
                                size = Size(width, height),
                                cornerRadius = CornerRadius(width / 2f),
                            )
                        }
                    }
                }
                else {
                    drawLine(lineColor, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), strokeWidth)
                    if (hovered || dragging) {
                        val width = indicatorLength.toPx().coerceAtLeast(0f).coerceAtMost(size.width)
                        val height = indicatorThickness.toPx().coerceAtLeast(0f).coerceAtMost(size.height)
                        if (width > 0f && height > 0f) {
                            drawRoundRect(
                                color = if (dragging) activeColor else indicatorColor,
                                topLeft = Offset((size.width - width) / 2f, (size.height - height) / 2f),
                                size = Size(width, height),
                                cornerRadius = CornerRadius(height / 2f),
                            )
                        }
                    }
                }
            },
    )
}
