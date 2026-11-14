package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.ui.node.pointerIcon

@Composable
private fun ComparatorHandle(
    horizontal: Boolean,
    splitRatio: Float,
    layoutDirection: LayoutDirection,
    hovered: Boolean,
    dragging: Boolean
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

    Box(modifier = Modifier.fillMaxSize().drawWithContent {
        val axisExtent = if (horizontal) size.width else size.height
        val boundedRatio = constrainedDividerRatio(splitRatio, axisExtent, handleSizeDp.toPx())
        val splitPosition = if (horizontal && layoutDirection == LayoutDirection.Rtl) size.width * (1f - boundedRatio) else axisExtent * boundedRatio

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
    })
}

@Composable
private fun ComparatorPane(
    horizontal: Boolean,
    firstPane: Boolean,
    splitRatio: Float,
    layoutDirection: LayoutDirection,
    content: @Composable BoxScope.() -> Unit,
) {
    val gripSizeDp = Theme.size.input8

    Box(
        modifier = Modifier.fillMaxSize().drawWithContent {
            val axisExtent = if (horizontal) size.width else size.height
            val boundedRatio = constrainedDividerRatio(splitRatio, axisExtent, gripSizeDp.toPx())

            if (horizontal) {
                val splitPosition = size.width * if (layoutDirection == LayoutDirection.Rtl) 1f - boundedRatio else boundedRatio
                val showLeftSide = firstPane == (layoutDirection == LayoutDirection.Ltr)
                if (showLeftSide) clipRect(right = splitPosition) { this@drawWithContent.drawContent() }
                else clipRect(left = splitPosition) { this@drawWithContent.drawContent() }
            }
            else {
                val splitPosition = size.height * boundedRatio
                if (firstPane) clipRect(bottom = splitPosition) { this@drawWithContent.drawContent() }
                else clipRect(top = splitPosition) { this@drawWithContent.drawContent() }
            }
        },
        content = content
    )
}

@Composable
private fun ComparatorLayout(
    horizontal: Boolean,
    modifier: Modifier,
    first: @Composable BoxScope.() -> Unit,
    second: @Composable BoxScope.() -> Unit
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
                gripSize = Theme.size.input8,
                ratio = { splitRatio },
                onRatioChange = { splitRatio = it },
                onHoverChange = { hovered = it },
                onDraggingChange = { dragging = it },
                hitTargetSize = hitTargetSize,
                layoutDirection = layoutDirection,
            ),
        content = {
            ComparatorPane(horizontal, true, splitRatio, layoutDirection, first)
            ComparatorPane(horizontal, false, splitRatio, layoutDirection, second)
            ComparatorHandle(horizontal, splitRatio, layoutDirection, hovered, dragging)
        }
    ) { measurables, constraints ->
        val fallbackExtent = fallbackSize.roundToPx().coerceAtLeast(0)
        val width = constraints.constrainWidth(if (constraints.hasBoundedWidth) constraints.maxWidth else maxOf(fallbackExtent, constraints.minWidth))
        val height = constraints.constrainHeight(if (constraints.hasBoundedHeight) constraints.maxHeight else maxOf(fallbackExtent, constraints.minHeight))
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
    left: @Composable BoxScope.() -> Unit,
    right: @Composable BoxScope.() -> Unit
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
    top: @Composable BoxScope.() -> Unit,
    bottom: @Composable BoxScope.() -> Unit
) {
    ComparatorLayout(horizontal = false, modifier = modifier, first = top, second = bottom)
}