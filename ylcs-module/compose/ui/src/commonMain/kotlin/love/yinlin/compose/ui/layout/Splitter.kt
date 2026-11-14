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

private fun DrawScope.drawDividerTrack(
    horizontal: Boolean,
    position: Float,
    color: Color,
    surfaceColor: Color,
    thickness: Float,
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

private fun dividerReservedExtent(
    axisExtent: Int,
    indicatorExtent: Int,
    marginExtent: Int,
    overlay: Boolean,
): Int {
    if (overlay) return 0
    val handleExtent = indicatorExtent.coerceIn(0, axisExtent)
    val margin = marginExtent.coerceAtLeast(0).coerceAtMost(((axisExtent - handleExtent) / 2).coerceAtLeast(0))
    return handleExtent + margin * 2
}

private fun dividerCenter(
    axisExtent: Int,
    reservedExtent: Int,
    ratio: Float,
    horizontal: Boolean,
    layoutDirection: LayoutDirection,
): Float {
    val center = (axisExtent - reservedExtent) * ratio + reservedExtent / 2f
    return if (horizontal && layoutDirection == LayoutDirection.Rtl) axisExtent - center else center
}

/** Splitter reserves its track; Comparator overlays the track without changing content measurement. */
private fun Modifier.dividerGestures(
    horizontal: Boolean,
    overlay: Boolean,
    ratio: () -> Float,
    onRatioChange: (Float) -> Unit,
    onHoverChange: (Boolean) -> Unit,
    onDraggingChange: (Boolean) -> Unit,
    indicatorSize: Dp,
    indicatorMargin: Dp,
    hitTargetSize: Dp,
    layoutDirection: LayoutDirection,
): Modifier = this
    .pointerInput(horizontal, overlay, indicatorSize, indicatorMargin, hitTargetSize, layoutDirection) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                when (event.type) {
                    PointerEventType.Exit -> onHoverChange(false)
                    PointerEventType.Enter, PointerEventType.Move -> {
                        val change = event.changes.firstOrNull() ?: continue
                        val axisExtent = if (horizontal) size.width else size.height
                        val inside = change.position.x >= 0f && change.position.x < size.width &&
                            change.position.y >= 0f && change.position.y < size.height
                        val reservedExtent = dividerReservedExtent(axisExtent, indicatorSize.roundToPx(), indicatorMargin.roundToPx(), overlay)
                        val center = dividerCenter(axisExtent, reservedExtent, ratio(), horizontal, layoutDirection)
                        val hitExtent = maxOf(hitTargetSize.roundToPx(), if (overlay) indicatorSize.roundToPx() else reservedExtent)
                        val pointerAxis = if (horizontal) change.position.x else change.position.y
                        onHoverChange(change.type == PointerType.Mouse && inside && axisExtent > 0 && abs(pointerAxis - center) <= hitExtent / 2f)
                    }
                    else -> Unit
                }
            }
        }
    }
    .pointerInput(horizontal, overlay, indicatorSize, indicatorMargin, hitTargetSize, layoutDirection) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val axisExtent = if (horizontal) size.width else size.height
            if (axisExtent <= 0) return@awaitEachGesture

            val reservedExtent = dividerReservedExtent(axisExtent, indicatorSize.roundToPx(), indicatorMargin.roundToPx(), overlay)
            val center = dividerCenter(axisExtent, reservedExtent, ratio(), horizontal, layoutDirection)
            val hitExtent = maxOf(hitTargetSize.roundToPx(), if (overlay) indicatorSize.roundToPx() else reservedExtent)
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
                    val currentReservedExtent = dividerReservedExtent(currentExtent, indicatorSize.roundToPx(), indicatorMargin.roundToPx(), overlay)
                    val availableExtent = currentExtent - currentReservedExtent
                    if (availableExtent > 0) {
                        val pointerAxis = if (horizontal) change.position.x else change.position.y
                        val visualRatio = ((pointerAxis - grabOffset - currentReservedExtent / 2f) / availableExtent).coerceIn(0f, 1f)
                        val logicalRatio = if (horizontal && layoutDirection == LayoutDirection.Rtl) 1f - visualRatio else visualRatio
                        val nextRatio = if (overlay) logicalRatio else logicalRatio.coerceIn(SPLITTER_MIN_RATIO, SPLITTER_MAX_RATIO)
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

@Composable
private fun SplitterHandle(horizontal: Boolean, hovered: Boolean, dragging: Boolean) {
    val active = hovered || dragging
    val primaryColor = Theme.color.primary
    val outlineColor = Theme.color.outline
    val surfaceColor = Theme.color.surface
    val lineThicknessDp = Theme.border.v6
    val handleSizeDp = Theme.size.input9
    val shadowOffsetDp = Theme.shadow.v7
    val borderWidthDp = Theme.border.v7

    Box(
        modifier = Modifier.drawWithContent {
            drawContent()
            val lineColor = if (active) primaryColor else outlineColor
            val trackPosition = if (horizontal) size.width / 2f else size.height / 2f
            val lineThickness = lineThicknessDp.toPx().coerceAtLeast(1f)
            if (horizontal) drawLine(lineColor, Offset(trackPosition, 0f), Offset(trackPosition, size.height), lineThickness)
            else drawLine(lineColor, Offset(0f, trackPosition), Offset(size.width, trackPosition), lineThickness)

            val pillWidth = if (horizontal) size.width else handleSizeDp.toPx().coerceAtMost(size.width)
            val pillHeight = if (horizontal) handleSizeDp.toPx().coerceAtMost(size.height) else size.height
            if (active && pillWidth > 0f && pillHeight > 0f) {
                val topLeft = Offset((size.width - pillWidth) / 2f, (size.height - pillHeight) / 2f)
                val pillSize = Size(pillWidth, pillHeight)
                val cornerRadius = CornerRadius(minOf(pillWidth, pillHeight) / 2f)
                drawRoundRect(Color.Black.copy(alpha = 0.14f), topLeft + Offset(0f, shadowOffsetDp.toPx()), pillSize, cornerRadius)
                drawRoundRect(surfaceColor, topLeft, pillSize, cornerRadius)
                drawRoundRect(lineColor, topLeft, pillSize, cornerRadius, style = Stroke(width = borderWidthDp.toPx()))

                val center = Offset(size.width / 2f, size.height / 2f)
                val strokeLength = minOf(pillWidth, pillHeight) * 0.38f
                val spacing = minOf(pillWidth, pillHeight) * 0.22f
                val strokeWidth = lineThicknessDp.toPx().coerceAtLeast(1f)
                for (index in -1 .. 1) {
                    if (horizontal) {
                        val y = center.y + spacing * index
                        drawLine(lineColor, Offset(center.x - strokeLength / 2f, y), Offset(center.x + strokeLength / 2f, y), strokeWidth, cap = StrokeCap.Round)
                    }
                    else {
                        val x = center.x + spacing * index
                        drawLine(lineColor, Offset(x, center.y - strokeLength / 2f), Offset(x, center.y + strokeLength / 2f), strokeWidth, cap = StrokeCap.Round)
                    }
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
            .pointerIcon(if (hovered || dragging) PointerIcon.Hand else PointerIcon.Default)
            .dividerGestures(
                horizontal = horizontal,
                overlay = false,
                ratio = { splitRatio },
                onRatioChange = { splitRatio = it },
                onHoverChange = { hovered = it },
                onDraggingChange = { dragging = it },
                indicatorSize = 0.dp,
                indicatorMargin = indicatorMargin,
                hitTargetSize = hitTargetSize,
                layoutDirection = layoutDirection,
            ),
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
        val marginExtent = indicatorMargin.roundToPx().coerceAtLeast(0).coerceAtMost(axisExtent / 2)
        val dividerExtent = marginExtent * 2
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
                handlePlaceable.placeRelative(firstExtent + marginExtent - handleExtent / 2, 0)
                secondPlaceable.placeRelative(firstExtent + dividerExtent, verticalAlignment.align(secondPlaceable.height, height))
            }
        }
        else {
            val width = constraints.constrainWidth(maxOf(firstPlaceable.width, secondPlaceable.width))
            val height = constraints.constrainHeight(axisExtent)
            val handlePlaceable = measurables[2].measure(Constraints.fixed(width, handleExtent))

            layout(width, height) {
                firstPlaceable.place(horizontalAlignment.align(firstPlaceable.width, width, layoutDirection), 0)
                handlePlaceable.place(0, firstExtent + marginExtent - handleExtent / 2)
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
 * @param indicatorMargin 两侧内容在分隔线位置的额外间距，默认为 0；抓手始终浮于内容上方
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
 * @param indicatorMargin 两侧内容在分隔线位置的额外间距，默认为 0；抓手始终浮于内容上方
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
    val onSurfaceColor = Theme.color.onSurface
    val lineThicknessDp = Theme.border.v6
    val handleSizeDp = Theme.size.input8
    val handleThicknessDp = Theme.size.input10
    val shadowOffsetDp = Theme.shadow.v5
    val borderWidthDp = Theme.border.v7

    Box(
        modifier = Modifier.fillMaxSize().drawWithContent {
            val splitPosition = if (horizontal && layoutDirection == LayoutDirection.Rtl) {
                size.width * (1f - splitRatio)
            }
            else if (horizontal) size.width * splitRatio
            else size.height * splitRatio

            val lineColor = if (active) primaryColor else outlineColor
            drawDividerTrack(horizontal, splitPosition, lineColor, surfaceColor, lineThicknessDp.toPx().coerceAtLeast(1f))

            val pillWidth = (if (horizontal) handleSizeDp else handleThicknessDp).toPx().coerceAtMost(size.width)
            val pillHeight = (if (horizontal) handleThicknessDp else handleSizeDp).toPx().coerceAtMost(size.height)
            if (pillWidth > 0f && pillHeight > 0f) {
                val center = if (horizontal) Offset(splitPosition, size.height / 2f) else Offset(size.width / 2f, splitPosition)
                val topLeft = center - Offset(pillWidth / 2f, pillHeight / 2f)
                val pillSize = Size(pillWidth, pillHeight)
                val cornerRadius = CornerRadius(minOf(pillWidth, pillHeight) / 2f)
                drawRoundRect(Color.Black.copy(alpha = 0.22f), topLeft + Offset(0f, shadowOffsetDp.toPx()), pillSize, cornerRadius)
                drawRoundRect(surfaceColor, topLeft, pillSize, cornerRadius)
                drawRoundRect(lineColor, topLeft, pillSize, cornerRadius, style = Stroke(width = borderWidthDp.toPx()))
                drawDirectionalArrows(horizontal, center, minOf(pillWidth, pillHeight), if (active) primaryColor else onSurfaceColor)
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
            .pointerIcon(if (hovered || dragging) PointerIcon.Hand else PointerIcon.Default)
            .dividerGestures(
                horizontal = horizontal,
                overlay = true,
                ratio = { splitRatio },
                onRatioChange = { splitRatio = it },
                onHoverChange = { hovered = it },
                onDraggingChange = { dragging = it },
                indicatorSize = Theme.size.input8,
                indicatorMargin = 0.dp,
                hitTargetSize = hitTargetSize,
                layoutDirection = layoutDirection,
            ),
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
