package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastRoundToInt
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.ui.node.pointerIcon

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
            drawDividerTrack(horizontal, trackPosition, lineColor, surfaceColor, lineThicknessDp.toPx().coerceAtLeast(1f))

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
    first: @Composable BoxScope.() -> Unit,
    second: @Composable BoxScope.() -> Unit,
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
                gripSize = indicatorSize,
                ratio = { splitRatio },
                onRatioChange = { splitRatio = it },
                onHoverChange = { hovered = it },
                onDraggingChange = { dragging = it },
                hitTargetSize = hitTargetSize,
                layoutDirection = layoutDirection,
            ),
        content = {
            Box(content = first)
            Box(content = second)
            SplitterHandle(horizontal, hovered, dragging)
        }
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
        val boundedRatio = constrainedDividerRatio(splitRatio, axisExtent.toFloat(), handleExtent.toFloat())
        val dividerPosition = axisExtent * boundedRatio
        val firstExtent = dividerPosition.fastRoundToInt().coerceIn(0, axisExtent)
        val secondExtent = axisExtent - firstExtent
        val handleStart = (dividerPosition - handleExtent / 2f).fastRoundToInt().coerceIn(0, axisExtent - handleExtent)

        val firstConstraints = if (horizontal) Constraints(maxWidth = firstExtent, maxHeight = crossExtent) else Constraints(maxWidth = crossExtent, maxHeight = firstExtent)
        val secondConstraints = if (horizontal) Constraints(maxWidth = secondExtent, maxHeight = crossExtent) else Constraints(maxWidth = crossExtent, maxHeight = secondExtent)

        val firstPlaceable = measurables[0].measure(firstConstraints)
        val secondPlaceable = measurables[1].measure(secondConstraints)

        if (horizontal) {
            val width = constraints.constrainWidth(axisExtent)
            val height = constraints.constrainHeight(maxOf(firstPlaceable.height, secondPlaceable.height))
            val handlePlaceable = measurables[2].measure(Constraints.fixed(handleExtent, height))

            layout(width, height) {
                firstPlaceable.placeRelative(0, verticalAlignment.align(firstPlaceable.height, height))
                secondPlaceable.placeRelative(firstExtent, verticalAlignment.align(secondPlaceable.height, height))
                handlePlaceable.placeRelative(handleStart, 0)
            }
        }
        else {
            val width = constraints.constrainWidth(maxOf(firstPlaceable.width, secondPlaceable.width))
            val height = constraints.constrainHeight(axisExtent)
            val handlePlaceable = measurables[2].measure(Constraints.fixed(width, handleExtent))

            layout(width, height) {
                firstPlaceable.place(horizontalAlignment.align(firstPlaceable.width, width, layoutDirection), 0)
                secondPlaceable.place(horizontalAlignment.align(secondPlaceable.width, width, layoutDirection), firstExtent)
                handlePlaceable.place(0, handleStart)
            }
        }
    }
}

/**
 * 将内容分成左右两个可调节区域
 *
 * @param modifier 分隔器尺寸与布局修饰符
 * @param left 起始侧内容，RTL 布局下位于右侧
 * @param right 末尾侧内容，RTL 布局下位于左侧
 * @param alignment 两侧内容的垂直对齐方式
 */
@Composable
fun HorizontalSplitter(
    modifier: Modifier = Modifier,
    left: @Composable BoxScope.() -> Unit,
    right: @Composable BoxScope.() -> Unit,
    alignment: Alignment.Vertical = Alignment.CenterVertically
) {
    Splitter(
        horizontal = true,
        modifier = modifier,
        verticalAlignment = alignment,
        horizontalAlignment = Alignment.CenterHorizontally,
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
 */
@Composable
fun VerticalSplitter(
    modifier: Modifier = Modifier,
    top: @Composable BoxScope.() -> Unit,
    bottom: @Composable BoxScope.() -> Unit,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally
) {
    Splitter(
        horizontal = false,
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = alignment,
        first = top,
        second = bottom
    )
}
