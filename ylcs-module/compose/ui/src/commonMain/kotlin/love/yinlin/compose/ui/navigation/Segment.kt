package love.yinlin.compose.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.layout.MeasureId
import love.yinlin.compose.ui.layout.find
import love.yinlin.compose.ui.layout.measureId
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.node.semantics
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.extension.then

private enum class SegmentMeasureId : MeasureId {
    Indicator, Item
}

@Composable
fun Segment(
    size: Int,
    index: Int,
    onNavigate: (Int) -> Unit,
    titleProvider: (Int) -> String,
    modifier: Modifier = Modifier,
    iconProvider: ((Int) -> ImageVector?)? = null,
    shape: Shape = Theme.shape.circle,
    padding: PaddingValues = Theme.padding.value9,
    style: TextStyle = LocalStyle.current.bold,
    onCurrentClick: ((Int) -> Unit)? = null,
    onLongClick: ((Int) -> Unit)? = null,
) {
    val animationDuration = Theme.animation.duration.default
    val indicatorPosition = if (index in 0 ..< size) animateFloatAsState(index.toFloat(), tween(animationDuration)) else null

    Surface(
        modifier = modifier.semantics(Role.Tab),
        contentPadding = PaddingValues(Theme.padding.g5),
        shape = shape,
        border = BorderStroke(Theme.border.v7, Theme.color.outline.copy(alpha = 0.15f)),
        propagateMinConstraints = true
    ) {
        Layout(
            content = {
                if (indicatorPosition != null) {
                    Surface(
                        modifier = Modifier.measureId(SegmentMeasureId.Indicator).semantics().zIndex(1f),
                        shape = shape,
                        tonalLevel = 1,
                        shadowElevation = Theme.shadow.v8,
                        border = BorderStroke(Theme.border.v7, Theme.color.outline.copy(alpha = 0.1f)),
                    ) { }
                }

                repeat(size) { i ->
                    val selected = index == i

                    TextIconAdapter(modifier = Modifier
                        .measureId(SegmentMeasureId.Item)
                        .clip(shape)
                        .pointerIcon(PointerIcon.Hand)
                        .combinedClickable(
                            onClick = {
                                if (selected) onCurrentClick?.invoke(i)
                                else onNavigate(i)
                            },
                            onLongClick = { onLongClick?.invoke(i) }
                        ).padding(padding)
                        .zIndex(2f)
                    ) { iconId, textId ->
                        val contentColor by animateColorAsState(
                            targetValue = if (selected) Theme.color.primary else Theme.color.onSurfaceVariant,
                            animationSpec = tween(animationDuration)
                        )
                        val icon = iconProvider?.invoke(i)

                        icon?.then { Icon(icon = it, color = contentColor, modifier = Modifier.iconId()) }
                        SimpleClipText(text = titleProvider(i), modifier = Modifier.textId(), color = contentColor, style = style)
                    }
                }
            }
        ) { measurables, constraints ->
            val indicator = measurables.find(SegmentMeasureId.Indicator)
            val items = measurables.filter { it.layoutId == SegmentMeasureId.Item }
            val count = items.size
            if (count == 0) return@Layout layout(constraints.minWidth, constraints.minHeight) {}

            val naturalWidths = if (constraints.hasFixedWidth) null else IntArray(count) { i ->
                items[i].maxIntrinsicWidth(constraints.maxHeight)
            }
            val naturalWidth = naturalWidths?.sum() ?: constraints.maxWidth
            val width = constraints.constrainWidth(naturalWidth)
            val equalWidth = width / count
            val extraPixels = width % count

            val offsets = IntArray(count + 1)
            val placeables = List(count) { i ->
                val extraPixel = if (i < extraPixels) 1 else 0
                val itemWidth = if (naturalWidths != null && width == naturalWidth) naturalWidths[i] else equalWidth + extraPixel
                val placeable = items[i].measure(constraints.copy(minWidth = itemWidth, maxWidth = itemWidth, minHeight = 0))
                offsets[i + 1] = offsets[i] + placeable.width
                placeable
            }

            val height = constraints.constrainHeight(placeables.maxOf { it.height })

            layout(width, height) {
                if (indicator != null && indicatorPosition != null) {
                    val position = indicatorPosition.value.coerceIn(0f, placeables.lastIndex.toFloat())
                    val from = position.toInt()
                    val to = minOf(from + 1, placeables.lastIndex)
                    val fraction = position - from
                    val left = lerp(offsets[from], offsets[to], fraction)
                    val right = lerp(offsets[from + 1], offsets[to + 1], fraction)
                    val indicatorHeight = lerp(placeables[from].height, placeables[to].height, fraction)
                    indicator.measure(Constraints.fixed(right - left, indicatorHeight)).placeRelative(left, (height - indicatorHeight) / 2)
                }

                for (i in placeables.indices) {
                    val placeable = placeables[i]
                    placeable.placeRelative(offsets[i], (height - placeable.height) / 2)
                }
            }
        }
    }
}
