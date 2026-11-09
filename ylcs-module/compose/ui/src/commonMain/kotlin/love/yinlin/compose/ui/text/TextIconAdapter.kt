package love.yinlin.compose.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import love.yinlin.compose.ui.layout.MeasureId
import love.yinlin.compose.ui.layout.MeasureIdProvider
import love.yinlin.compose.ui.layout.find
import love.yinlin.compose.ui.layout.provider
import love.yinlin.compose.ui.layout.require

private enum class TextIconAdapterMeasureId : MeasureId {
    Text, Icon;
}

/**
 * 图标自适应与文本同高
 *
 * @param ltr 从左向右: 图标 -> 文字
 * @param gapRatio 间隔比例
 * @param content idText必须附加到Text上, idIcon可选
 */
@Composable
fun TextIconAdapter(
    modifier: Modifier = Modifier,
    ltr: Boolean = true,
    gapRatio: Float = 0.5f,
    content: @Composable (idIcon: MeasureIdProvider, idText: MeasureIdProvider) -> Unit,
) {
    Layout(
        modifier = modifier,
        content = { content(TextIconAdapterMeasureId.Icon::provider, TextIconAdapterMeasureId.Text::provider) },
        measurePolicy = remember(ltr, gapRatio) { TextIconAdapterMeasurePolicy(ltr, gapRatio) },
    )
}

private class TextIconAdapterMeasurePolicy(
    private val ltr: Boolean,
    private val gapRatio: Float,
) : MeasurePolicy {
    private fun iconWidth(textHeight: Int, availableWidth: Int): Int = if (textHeight >= Short.MAX_VALUE) 0 else minOf(textHeight, availableWidth)

    private fun iconGap(textHeight: Int, iconWidth: Int, availableWidth: Int): Int = if (iconWidth == 0) 0 else (textHeight * gapRatio).toInt().coerceIn(-iconWidth, availableWidth - iconWidth)

    private fun intrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int, max: Boolean): Int {
        val text = measurables.require(TextIconAdapterMeasureId.Text)
        val textWidth = if (max) text.maxIntrinsicWidth(height) else text.minIntrinsicWidth(height)
        if (measurables.find(TextIconAdapterMeasureId.Icon) == null) return textWidth

        val textHeight = minOf(text.maxIntrinsicHeight(Constraints.Infinity), height)
        val availableWidth = Constraints.Infinity - textWidth
        val iconWidth = iconWidth(textHeight, availableWidth)

        return textWidth + iconWidth + iconGap(textHeight, iconWidth, availableWidth)
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int = intrinsicWidth(measurables, height, max = false)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int = intrinsicWidth(measurables, height, max = true)

    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val textMeasurable = measurables.require(TextIconAdapterMeasureId.Text)
        val iconMeasurable = measurables.find(TextIconAdapterMeasureId.Icon)
        val reservedWidth = if (iconMeasurable != null && constraints.hasBoundedWidth) {
            val textHeight = minOf(textMeasurable.maxIntrinsicHeight(Constraints.Infinity), constraints.maxHeight)
            val iconWidth = iconWidth(textHeight, constraints.maxWidth)
            iconWidth + iconGap(textHeight, iconWidth, constraints.maxWidth)
        } else 0

        val textPlaceable = textMeasurable.measure(constraints.copy(minWidth = 0, maxWidth = constraints.maxWidth - reservedWidth, minHeight = 0))
        val textWidth = textPlaceable.width
        val textHeight = textPlaceable.height
        val availableWidth = constraints.maxWidth - textWidth
        val iconWidth = if (iconMeasurable == null) 0 else iconWidth(textHeight, availableWidth)
        val iconPlaceable = iconMeasurable?.measure(Constraints.fixed(iconWidth, iconWidth))
        val gap = iconGap(textHeight, iconWidth, availableWidth)
        val contentWidth = textWidth + iconWidth + gap
        val width = constraints.constrainWidth(contentWidth)
        val height = constraints.constrainHeight(textHeight)

        return layout(width, height) {
            val start = (width - contentWidth) / 2
            val textY = (height - textHeight) / 2
            val iconY = (height - iconWidth) / 2

            if (ltr) {
                iconPlaceable?.placeRelative(start, iconY)
                textPlaceable.placeRelative(start + iconWidth + gap, textY)
            }
            else {
                textPlaceable.placeRelative(start, textY)
                iconPlaceable?.placeRelative(start + textWidth + gap, iconY)
            }
        }
    }
}

/**
 * 图标自适应与文本视觉同高(实际上图标会略大一些1.2x)
 *
 * @param ttb 从上往下: 图标 -> 文字
 * @param gapRatio 间隔比例
 * @param content idText必须附加到Text上, idIcon可选
 */
@Composable
fun TextIconBinder(
    modifier: Modifier = Modifier,
    ttb: Boolean = true,
    gapRatio: Float = 0.5f,
    content: @Composable (idIcon: MeasureIdProvider, idText: MeasureIdProvider) -> Unit,
) {
    Layout(
        modifier = modifier,
        content = { content(TextIconAdapterMeasureId.Icon::provider, TextIconAdapterMeasureId.Text::provider) },
    ) { measurables, constraints ->
        val textMeasurable = measurables.require(TextIconAdapterMeasureId.Text)
        val textPlaceable = textMeasurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val textHeight = textPlaceable.height
        val iconMeasurable = measurables.find(TextIconAdapterMeasureId.Icon)
        val iconSize = if (textHeight >= Short.MAX_VALUE) 0 else textHeight * 6 / 5
        val iconPlaceable = iconMeasurable?.measure(Constraints.fixed(iconSize, iconSize))

        val iconHeight = iconPlaceable?.height ?: 0
        val gap = if (iconPlaceable == null) 0 else (textHeight * gapRatio).toInt()

        val totalHeight = textHeight + iconHeight + gap
        val maxWidth = maxOf(textPlaceable.width, iconPlaceable?.width ?: 0)

        layout(maxWidth, totalHeight) {
            val textX = (maxWidth - textPlaceable.width) / 2
            val iconX = (maxWidth - (iconPlaceable?.width ?: 0)) / 2

            if (ttb) {
                iconPlaceable?.placeRelative(iconX, 0)
                textPlaceable.placeRelative(textX, iconHeight + gap)
            }
            else {
                textPlaceable.placeRelative(textX, 0)
                iconPlaceable?.placeRelative(iconX, textHeight + gap)
            }
        }
    }
}
