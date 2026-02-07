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

private enum class TextIconAdapterMeasureId : MeasureId {
    Text, Other;
}

@Composable
fun TextIconAdapter(
    modifier: Modifier = Modifier,
    ltr: Boolean = true,
    gapRatio: Float = 0.5f,
    content: @Composable (idIcon: MeasureIdProvider, idText: MeasureIdProvider) -> Unit,
) {
    Layout(
        modifier = modifier,
        content = { content(TextIconAdapterMeasureId.Other::provider, TextIconAdapterMeasureId.Text::provider) },
    ) { measurables, constraints ->
        val textMeasurable = measurables.require(TextIconAdapterMeasureId.Text)
        val textPlaceable = textMeasurable.measure(constraints)
        val heightPx = textPlaceable.height
        val iconMeasurable = measurables.find(TextIconAdapterMeasureId.Other)
        val iconPlaceable = iconMeasurable?.measure(Constraints.fixed(heightPx, heightPx))
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