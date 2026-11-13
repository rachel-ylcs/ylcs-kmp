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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.extension.cleaning
import kotlin.math.abs

private const val SPLITTER_MIN_RATIO = 0.05f
private const val SPLITTER_MAX_RATIO = 0.95f

private fun DrawScope.drawDirectionalArrows(
    horizontal: Boolean,
    center: Offset,
    diameter: Float,
    color: Color,
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

@Composable
private fun SplitterHandle(horizontal: Boolean, hovered: Boolean, dragging: Boolean) {
    val active = hovered || dragging
    val primaryColor = Theme.color.primary
    val outlineColor = Theme.color.outline
    val surfaceColor = Theme.color.surface
    val primaryContainerColor = Theme.color.primaryContainer
    val onContainerColor = Theme.color.onContainer
    val lineThicknessDp = Theme.border.v6
    val handleSizeDp = Theme.size.input9
    val shadowOffsetDp = Theme.shadow.v7
    val borderWidthDp = Theme.border.v6
    val circleSizeDp = Theme.size.box2
    val circleBorderWidthDp = Theme.border.v7

    Box(
        modifier = Modifier.drawWithContent {
            drawContent()

            val lineColor = if (active) primaryColor else outlineColor
            val lineThickness = lineThicknessDp.toPx().coerceAtLeast(1f)
            if (horizontal) {
                val x = size.width / 2f
                drawLine(surfaceColor, Offset(x, 0f), Offset(x, size.height), lineThickness + 3f)
                drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), lineThickness)
            }
            else {
                val y = size.height / 2f
                drawLine(surfaceColor, Offset(0f, y), Offset(size.width, y), lineThickness + 3f)
                drawLine(lineColor, Offset(0f, y), Offset(size.width, y), lineThickness)
            }

            val pillWidth = if (horizontal) size.width else handleSizeDp.toPx().coerceAtMost(size.width)
            val pillHeight = if (horizontal) handleSizeDp.toPx().coerceAtMost(size.height) else size.height
            if (pillWidth > 0f && pillHeight > 0f) {
                val topLeft = Offset((size.width - pillWidth) / 2f, (size.height - pillHeight) / 2f)
                val pillSize = Size(pillWidth, pillHeight)
                val cornerRadius = CornerRadius(minOf(pillWidth, pillHeight) / 2f)
                val shadowOffset = shadowOffsetDp.toPx()
                val borderWidth = borderWidthDp.toPx()

                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.2f),
                    topLeft = topLeft + Offset(0f, shadowOffset),
                    size = pillSize,
                    cornerRadius = cornerRadius,
                )
                drawRoundRect(surfaceColor, topLeft, pillSize, cornerRadius)
                drawRoundRect(
                    color = if (active) primaryColor else outlineColor,
                    topLeft = topLeft,
                    size = pillSize,
                    cornerRadius = cornerRadius,
                    style = Stroke(width = borderWidth),
                )

                val circleDiameter = minOf(circleSizeDp.toPx(), pillWidth, pillHeight)
                if (circleDiameter > 0f) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = circleDiameter / 2f
                    drawCircle(primaryContainerColor, radius, center)
                    drawCircle(primaryColor, radius, center, style = Stroke(width = circleBorderWidthDp.toPx()))
                    drawDirectionalArrows(horizontal, center, circleDiameter, onContainerColor)
                }
            }
        }
    )
}

@Composable
private fun Splitter(
    horizontal: Boolean,
    modifier: Modifier,
    verticalAlignment: Alignment.Vertical,
    horizontalAlignment: Alignment.Horizontal,
    indicatorMargin: Dp,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
) {
    var splitRatio by rememberValueState(0.5f, horizontal)
    var hovered by rememberFalse(horizontal)
    var dragging by rememberFalse(horizontal)
    val fallbackSize = Theme.size.cell1
    val indicatorSize = Theme.size.box1
    val hitTargetSize = Theme.size.input9
    val layoutDirection = LocalLayoutDirection.current
    val layoutModifier = if (horizontal) modifier.fillMaxWidth() else modifier.fillMaxHeight()

    Layout(
        modifier = layoutModifier
            .pointerIcon(if (hovered) PointerIcon.Hand else PointerIcon.Default)
            .pointerInput(horizontal, indicatorSize, indicatorMargin, layoutDirection) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        when (event.type) {
                            PointerEventType.Exit -> hovered = false
                            PointerEventType.Enter, PointerEventType.Move -> {
                                val change = event.changes.firstOrNull() ?: continue
                                if (change.type == PointerType.Mouse) {
                                    val axisExtent = if (horizontal) size.width else size.height
                                    val handleExtent = indicatorSize.roundToPx().coerceAtLeast(0).coerceAtMost(axisExtent)
                                    val marginExtent = indicatorMargin.roundToPx().coerceAtLeast(0)
                                        .coerceAtMost(((axisExtent - handleExtent) / 2).coerceAtLeast(0))
                                    val dividerExtent = handleExtent + marginExtent * 2
                                    val availableExtent = (axisExtent - dividerExtent).coerceAtLeast(0)
                                    val firstExtent = (availableExtent * splitRatio.coerceIn(SPLITTER_MIN_RATIO, SPLITTER_MAX_RATIO))
                                        .fastRoundToInt()
                                        .coerceIn(0, availableExtent)
                                    val dividerCenter = if (horizontal && layoutDirection == LayoutDirection.Rtl) {
                                        axisExtent - firstExtent - dividerExtent / 2f
                                    }
                                    else firstExtent + dividerExtent / 2f
                                    val hitTargetExtent = maxOf(dividerExtent, hitTargetSize.roundToPx())
                                    val pointerAxis = if (horizontal) change.position.x else change.position.y
                                    hovered = axisExtent > 0 && abs(pointerAxis - dividerCenter) <= hitTargetExtent / 2f
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }.pointerInput(horizontal, indicatorSize, indicatorMargin, layoutDirection) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val axisExtent = if (horizontal) size.width else size.height
                    if (axisExtent <= 0) return@awaitEachGesture

                    val handleExtent = indicatorSize.roundToPx().coerceAtLeast(0).coerceAtMost(axisExtent)
                    val marginExtent = indicatorMargin.roundToPx().coerceAtLeast(0)
                        .coerceAtMost(((axisExtent - handleExtent) / 2).coerceAtLeast(0))
                    val dividerExtent = handleExtent + marginExtent * 2
                    val availableExtent = (axisExtent - dividerExtent).coerceAtLeast(0)
                    val firstExtent = (availableExtent * splitRatio.coerceIn(SPLITTER_MIN_RATIO, SPLITTER_MAX_RATIO))
                        .fastRoundToInt()
                        .coerceIn(0, availableExtent)
                    val dividerCenter = if (horizontal && layoutDirection == LayoutDirection.Rtl) {
                        axisExtent - firstExtent - dividerExtent / 2f
                    }
                    else firstExtent + dividerExtent / 2f
                    val hitTargetExtent = maxOf(dividerExtent, hitTargetSize.roundToPx())
                    val downAxis = if (horizontal) down.position.x else down.position.y
                    if (abs(downAxis - dividerCenter) > hitTargetExtent / 2f) return@awaitEachGesture

                    dragging = true
                    cleaning({
                        down.consume()
                        var previous = downAxis

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (
                                event.type == PointerEventType.Exit ||
                                change.position.x < 0f ||
                                change.position.x >= size.width.toFloat() ||
                                change.position.y < 0f ||
                                change.position.y >= size.height.toFloat()
                            ) {
                                hovered = false
                                return@awaitEachGesture
                            }

                            val current = if (horizontal) change.position.x else change.position.y
                            val delta = current - previous
                            val currentAxisExtent = if (horizontal) size.width else size.height
                            val currentHandleExtent = indicatorSize.roundToPx().coerceAtLeast(0).coerceAtMost(currentAxisExtent)
                            val currentMarginExtent = indicatorMargin.roundToPx().coerceAtLeast(0)
                                .coerceAtMost(((currentAxisExtent - currentHandleExtent) / 2).coerceAtLeast(0))
                            val currentDividerExtent = currentHandleExtent + currentMarginExtent * 2
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
                    }) {
                        dragging = false
                    }
                }
            },
        content = {
            Box(content = { first() })
            Box(content = { second() })
            SplitterHandle(horizontal, hovered, dragging)
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
        val handleExtent = indicatorSize.roundToPx().coerceAtLeast(0).coerceAtMost(axisExtent)
        val marginExtent = indicatorMargin.roundToPx().coerceAtLeast(0)
            .coerceAtMost(((axisExtent - handleExtent) / 2).coerceAtLeast(0))
        val dividerExtent = handleExtent + marginExtent * 2
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
            val handlePlaceable = measurables[2].measure(Constraints.fixed(handleExtent, height))

            layout(width, height) {
                firstPlaceable.placeRelative(0, verticalAlignment.align(firstPlaceable.height, height))
                handlePlaceable.placeRelative(firstExtent + marginExtent, 0)
                secondPlaceable.placeRelative(firstExtent + dividerExtent, verticalAlignment.align(secondPlaceable.height, height))
            }
        }
        else {
            val width = constraints.constrainWidth(maxOf(firstPlaceable.width, secondPlaceable.width))
            val height = constraints.constrainHeight(axisExtent)
            val handlePlaceable = measurables[2].measure(Constraints.fixed(width, handleExtent))

            layout(width, height) {
                firstPlaceable.place(horizontalAlignment.align(firstPlaceable.width, width, layoutDirection), 0)
                handlePlaceable.place(0, firstExtent + marginExtent)
                secondPlaceable.place(horizontalAlignment.align(secondPlaceable.width, width, layoutDirection), firstExtent + dividerExtent)
            }
        }
    }
}

/**
 * 将内容分成左右两个可调节区域。
 *
 * @param modifier 分隔器尺寸与布局修饰符
 * @param left 起始侧内容，RTL 布局下位于右侧
 * @param right 末尾侧内容，RTL 布局下位于左侧
 * @param alignment 两侧内容的垂直对齐方式
 * @param indicatorMargin 指示器与两侧内容之间的间距，默认为 0
 */
@Composable
fun HorizontalSplitter(
    modifier: Modifier = Modifier,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
    alignment: Alignment.Vertical = Alignment.CenterVertically,
    indicatorMargin: Dp = 0.dp,
) {
    Splitter(
        horizontal = true,
        modifier = modifier,
        verticalAlignment = alignment,
        horizontalAlignment = Alignment.CenterHorizontally,
        indicatorMargin = indicatorMargin,
        first = left,
        second = right
    )
}

/**
 * 将内容分成上下两个可调节区域
 *
 * @param modifier 分隔器尺寸与布局修饰符
 * @param top 上方内容
 * @param bottom 下方内容
 * @param alignment 上下内容的水平对齐方式
 * @param indicatorMargin 指示器与两侧内容之间的间距，默认为 0
 */
@Composable
fun VerticalSplitter(
    modifier: Modifier = Modifier,
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    indicatorMargin: Dp = 0.dp,
) {
    Splitter(
        horizontal = false,
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = alignment,
        indicatorMargin = indicatorMargin,
        first = top,
        second = bottom
    )
}

@Composable
private fun ComparatorHandle(
    horizontal: Boolean,
    splitRatio: Float,
    layoutDirection: LayoutDirection,
    hovered: Boolean,
    dragging: Boolean,
) {
    val active = hovered || dragging
    val primaryColor = Theme.color.primary
    val outlineColor = Theme.color.outline
    val surfaceColor = Theme.color.surface
    val primaryContainerColor = Theme.color.primaryContainer
    val onContainerColor = Theme.color.onContainer
    val lineThicknessDp = Theme.border.v6
    val handleSizeDp = Theme.size.input8
    val shadowOffsetDp = Theme.shadow.v5
    val borderWidthDp = Theme.border.v6
    val innerBorderWidthDp = Theme.border.v7

    Box(
        modifier = Modifier.fillMaxSize().drawWithContent {
            val splitPosition = if (horizontal && layoutDirection == LayoutDirection.Rtl) {
                size.width * (1f - splitRatio)
            }
            else if (horizontal) size.width * splitRatio
            else size.height * splitRatio

            val lineColor = if (active) primaryColor else outlineColor
            val lineThickness = lineThicknessDp.toPx().coerceAtLeast(1f)
            if (horizontal) {
                drawLine(surfaceColor, Offset(splitPosition, 0f), Offset(splitPosition, size.height), lineThickness + 4f)
                drawLine(lineColor, Offset(splitPosition, 0f), Offset(splitPosition, size.height), lineThickness)
            }
            else {
                drawLine(surfaceColor, Offset(0f, splitPosition), Offset(size.width, splitPosition), lineThickness + 4f)
                drawLine(lineColor, Offset(0f, splitPosition), Offset(size.width, splitPosition), lineThickness)
            }

            val diameter = handleSizeDp.toPx().coerceAtMost(minOf(size.width, size.height))
            if (diameter > 0f) {
                val center = if (horizontal) Offset(splitPosition, size.height / 2f) else Offset(size.width / 2f, splitPosition)
                val radius = diameter / 2f
                drawCircle(
                    color = Color.Black.copy(alpha = 0.28f),
                    radius = radius,
                    center = center + Offset(0f, shadowOffsetDp.toPx()),
                )
                drawCircle(surfaceColor, radius, center)
                drawCircle(
                    color = if (active) primaryColor else outlineColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = borderWidthDp.toPx()),
                )

                val innerRadius = diameter * 0.31f
                drawCircle(primaryContainerColor, innerRadius, center)
                drawCircle(primaryColor, innerRadius, center, style = Stroke(width = innerBorderWidthDp.toPx()))
                drawDirectionalArrows(horizontal, center, innerRadius * 2f, onContainerColor)
            }
        },
    )
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
        }
    ) {
        content()
    }
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
    val hitTargetSize = Theme.size.input7
    val layoutDirection = LocalLayoutDirection.current
    val layoutModifier = if (horizontal) modifier.fillMaxWidth() else modifier.fillMaxHeight()

    Layout(
        modifier = layoutModifier
            .pointerIcon(if (hovered) PointerIcon.Hand else PointerIcon.Default)
            .pointerInput(horizontal, hitTargetSize, layoutDirection) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        when (event.type) {
                            PointerEventType.Exit -> hovered = false
                            PointerEventType.Enter, PointerEventType.Move -> {
                                val change = event.changes.firstOrNull() ?: continue
                                if (change.type == PointerType.Mouse) {
                                    val axisExtent = if (horizontal) size.width else size.height
                                    val splitPosition = if (horizontal && layoutDirection == LayoutDirection.Rtl) {
                                        axisExtent * (1f - splitRatio)
                                    }
                                    else axisExtent * splitRatio
                                    val pointerPosition = if (horizontal) change.position.x else change.position.y
                                    hovered = axisExtent > 0 && abs(pointerPosition - splitPosition) <= hitTargetSize.roundToPx() / 2f
                                }
                            }
                            else -> Unit
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
                    cleaning({
                        down.consume()
                        var previousPosition = downPosition

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (
                                event.type == PointerEventType.Exit ||
                                change.position.x < 0f ||
                                change.position.x >= size.width.toFloat() ||
                                change.position.y < 0f ||
                                change.position.y >= size.height.toFloat()
                            ) {
                                hovered = false
                                return@awaitEachGesture
                            }

                            val currentPosition = if (horizontal) change.position.x else change.position.y
                            val currentAxisExtent = if (horizontal) size.width else size.height
                            val edgeThreshold = 1f
                            val atFirstEdge = if (horizontal && layoutDirection == LayoutDirection.Rtl) {
                                currentPosition >= currentAxisExtent - edgeThreshold
                            }
                            else currentPosition <= edgeThreshold
                            val atSecondEdge = if (horizontal && layoutDirection == LayoutDirection.Rtl) {
                                currentPosition <= edgeThreshold
                            }
                            else currentPosition >= currentAxisExtent - edgeThreshold
                            val delta = currentPosition - previousPosition
                            val logicalDelta = if (horizontal && layoutDirection == LayoutDirection.Rtl) -delta else delta
                            if (atFirstEdge) {
                                splitRatio = 0f
                                change.consume()
                            }
                            else if (atSecondEdge) {
                                splitRatio = 1f
                                change.consume()
                            }
                            else if (logicalDelta != 0f) {
                                splitRatio = (splitRatio + logicalDelta / currentAxisExtent).coerceIn(0f, 1f)
                                change.consume()
                            }

                            previousPosition = currentPosition
                            if (!change.pressed) break
                        }
                    }) {
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

/**
 * 将内容叠放为左右两部分，通过拖动中间的比较线调整两侧的可见范围
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
    ComparatorLayout(horizontal = true, modifier = modifier, first = left, second = right)
}

/**
 * 将内容叠放为上下两部分，通过拖动中间的比较线调整上下两侧的可见范围
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
    ComparatorLayout(horizontal = false, modifier = modifier, first = top, second = bottom)
}
