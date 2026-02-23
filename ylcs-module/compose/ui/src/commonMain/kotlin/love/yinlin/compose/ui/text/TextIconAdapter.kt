package love.yinlin.compose.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import love.yinlin.compose.ui.layout.MeasureId
import love.yinlin.compose.ui.layout.MeasureIdProvider
import love.yinlin.compose.ui.layout.find
import love.yinlin.compose.ui.layout.provider
import love.yinlin.compose.ui.layout.require
import kotlin.math.max

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
    ) { measurables, constraints ->
        val textMeasurable = measurables.require(TextIconAdapterMeasureId.Text)
        val textPlaceable = textMeasurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val heightPx = textPlaceable.height
        val iconSize = if (heightPx >= Short.MAX_VALUE) 0 else heightPx
        val iconMeasurable = measurables.find(TextIconAdapterMeasureId.Icon)
        val iconPlaceable = iconMeasurable?.measure(Constraints.fixed(iconSize, iconSize))
        val textWidth = textPlaceable.width
        val iconWidth = iconPlaceable?.width ?: 0
        val gap = if (iconPlaceable == null) 0 else (heightPx * gapRatio).toInt()

        layout(iconWidth + gap + textWidth, heightPx) {
            if (ltr) {
                iconPlaceable?.placeRelative(0, 0)
                textPlaceable.placeRelative(iconWidth + gap, 0)
            }
            else {
                iconPlaceable?.placeRelative(textWidth + gap, 0)
                textPlaceable.placeRelative(0, 0)
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
        val maxWidth = max(textPlaceable.width, iconPlaceable?.width ?: 0)

        layout(maxWidth, totalHeight) {
            val textX = (maxWidth - textPlaceable.width) / 2
            val iconX = (maxWidth - (iconPlaceable?.width ?: 0)) / 2

            if (ttb) {
                iconPlaceable?.placeRelative(iconX, 0)
                textPlaceable.placeRelative(textX, iconHeight + gap)
            } else {
                textPlaceable.placeRelative(textX, 0)
                iconPlaceable?.placeRelative(iconX, textHeight + gap)
            }
        }
    }
}