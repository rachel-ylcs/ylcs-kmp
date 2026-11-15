package love.yinlin.compose.ui.common

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.zIndex
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.TextIconBinder

@Composable
fun BoardIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    label: Int = 0,
    onClick: () -> Unit
) {
    Box(modifier = modifier) {
        Icon(icon = icon, onClick = onClick, modifier = Modifier.zIndex(1f))

        if (label > 0) {
            Box(
                modifier = Modifier.matchParentSize().zIndex(2f),
                contentAlignment = Alignment.TopEnd
            ) {
                Layout(
                    modifier = Modifier.background(color = Theme.color.error, shape = Theme.shape.circle),
                    content = {
                        SimpleClipText(
                            text = if (label < 10) label.toString() else "+",
                            style = Theme.typography.v8,
                            color = Theme.color.onError,
                            textAlign = TextAlign.Center
                        )
                    },
                    measurePolicy = BoardIconBadgeMeasurePolicy
                )
            }
        }
    }
}

private object BoardIconBadgeMeasurePolicy : MeasurePolicy {
    // 徽标只显示单个字符，固有边长取文字的自然宽高，再受另一轴限制。
    private fun intrinsicSize(text: IntrinsicMeasurable, maxSize: Int): Int {
        val width = text.maxIntrinsicWidth(Constraints.Infinity)
        val height = text.maxIntrinsicHeight(Constraints.Infinity)
        return minOf(maxOf(width, height), maxSize)
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int = intrinsicSize(measurables.first(), height)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int = intrinsicSize(measurables.first(), height)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int = intrinsicSize(measurables.first(), width)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int = intrinsicSize(measurables.first(), width)

    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val maxSize = minOf(constraints.maxWidth, constraints.maxHeight)
        val textConstraints = Constraints.fitPrioritizingWidth(minWidth = 0, maxWidth = maxSize, minHeight = 0, maxHeight = maxSize)
        val text = measurables.first().measure(textConstraints)
        val size = maxOf(text.width, text.height)
        return layout(size, size) {
            text.placeRelative(x = (size - text.width) / 2, y = (size - text.height) / 2)
        }
    }
}

@Composable
fun BoardButton(text: String, icon: ImageVector, label: Int = 0, onClick: () -> Unit) {
    Box {
        TextIconBinder(modifier = Modifier
            .clip(Theme.shape.v7)
            .clickable(onClick = onClick)
            .padding(Theme.padding.value)
            .zIndex(1f)
        ) { idIcon, idText ->
            Icon(icon = icon, modifier = Modifier.idIcon())
            SimpleEllipsisText(text = text, modifier = Modifier.idText())
        }
        if (label > 0) {
            Layout(
                modifier = Modifier
                    .background(color = Theme.color.error, shape = Theme.shape.circle)
                    .align(Alignment.TopEnd)
                    .zIndex(2f),
                content = {
                    SimpleClipText(
                        text = if (label < 10) label.toString() else "+",
                        color = Theme.color.onError,
                        textAlign = TextAlign.Center
                    )
                }
            ) { measurables, constraints ->
                val textPlaceable = measurables.first().measure(constraints)
                val boxSize = maxOf(textPlaceable.width, textPlaceable.height)
                layout(boxSize, boxSize) {
                    textPlaceable.placeRelative(x = (boxSize - textPlaceable.width) / 2, y = (boxSize - textPlaceable.height) / 2)
                }
            }
        }
    }
}

@Composable
fun BoardButtonContainer(title: String, content: @Composable RowScope.() -> Unit) {
    Surface(
        modifier = Modifier.padding(Theme.padding.value).fillMaxWidth(),
        contentPadding = Theme.padding.value9,
        shape = Theme.shape.v3,
        shadowElevation = Theme.shadow.v3
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
        ) {
            SimpleEllipsisText(text = title, style = Theme.typography.v6.bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}
