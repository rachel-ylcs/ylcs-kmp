package love.yinlin.compose.ui.image

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.zIndex
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.data.compose.Picture

/**
 * column / row:
 * 当前槽位左上角所在格
 *
 * columnSpan / rowSpan:
 * 当前槽位横跨多少格
 */
@Stable
private data class Slot(
    val column: Int,
    val row: Int,
    val columnSpan: Int = 1,
    val rowSpan: Int = 1
)

@Stable
private data class GridSpec(
    val columns: Int,
    val rows: Int,
    val slots: List<Slot>
)

private fun buildGridSpec(picSize: Int): GridSpec = when (picSize) {
    // □ □
    2 -> GridSpec(
        columns = 2,
        rows = 1,
        slots = [
            Slot(0, 0),
            Slot(1, 0)
        ]
    )
    // ┌──┬──┐
    // │ 1│ 2│
    // │  │──│
    // │  │ 3│
    // └──┴──┘
    3 -> GridSpec(
        columns = 2,
        rows = 2,
        slots = [
            Slot(0, 0, rowSpan = 2),
            Slot(1, 0),
            Slot(1, 1)
        ]
    )
    // 2 × 2
    4 -> GridSpec(
        columns = 2,
        rows = 2,
        slots = List(4) { index ->
            Slot(column = index % 2, row = index / 2)
        }
    )
    // ┌──┬──┬──┐
    // │  │ 2│ 3│
    // │ 1├──┼──┤
    // │  │ 4│ 5│
    // └──┴──┴──┘
    5 -> GridSpec(
        columns = 3,
        rows = 2,
        slots = [
            Slot(0, 0, rowSpan = 2),
            Slot(1, 0),
            Slot(2, 0),
            Slot(1, 1),
            Slot(2, 1)
        ]
    )
    // 3 × 2
    6 -> GridSpec(
        columns = 3,
        rows = 2,
        slots = List(6) { index ->
            Slot(
                column = index % 3,
                row = index / 3
            )
        }
    )
    // ┌──┬──┬──┐
    // │  │ 2│ 3│
    // │ 1├──┼──┤
    // │  │ 4│ 5│
    // │  ├──┼──┤
    // │  │ 6│ 7│
    // └──┴──┴──┘
    7 -> GridSpec(
        columns = 3,
        rows = 3,
        slots = [
            Slot(0, 0, rowSpan = 3),
            Slot(1, 0),
            Slot(2, 0),
            Slot(1, 1),
            Slot(2, 1),
            Slot(1, 2),
            Slot(2, 2)
        ]
    )
    // ┌──┬──┬──┐
    // │  │ 2│ 3│
    // │ 1├──┼──┤
    // │  │ 4│ 5│
    // ├──┼──┼──┤
    // │ 6│ 7│ 8│
    // └──┴──┴──┘
    8 -> GridSpec(
        columns = 3,
        rows = 3,
        slots = [
            Slot(0, 0, rowSpan = 2),
            Slot(1, 0),
            Slot(2, 0),
            Slot(1, 1),
            Slot(2, 1),
            Slot(0, 2),
            Slot(1, 2),
            Slot(2, 2)
        ]
    )
    // 3 × 3
    else -> GridSpec(
        columns = 3,
        rows = 3,
        slots = List(9) { index ->
            Slot(
                column = index % 3,
                row = index / 3
            )
        }
    )
}

@Composable
fun NineGrid(
    pics: List<Picture>,
    modifier: Modifier = Modifier,
    unique: Boolean = false,
    space: Dp = Theme.padding.g3,
    onImageClick: (Int, Picture) -> Unit = { _, _ -> },
    onVideoClick: (Picture) -> Unit = {},
    content: @Composable (ContentScale, Picture, () -> Unit) -> Unit
) {
    // 无图跳过
    val picSize = pics.size.fastCoerceAtMost(9)
    if (picSize == 0) return

    // 默认图片最小宽度
    val density = LocalDensity.current
    val minPicWidthPx = with(density) { Theme.size.image2.toPx().toInt() }
    val spacePx = with(density) { space.toPx().toInt().fastCoerceAtLeast(0) }

    Layout(
        modifier = modifier.clipToBounds(),
        content = {
            val contentScale = if (picSize == 1) ContentScale.Inside else ContentScale.Crop

            for (index in 0 ..< picSize) {
                val pic = pics[index]

                key(if (unique) pic.image else index) {
                    Box(contentAlignment = Alignment.Center) {
                        content(contentScale, pic) {
                            if (pic.isVideo) onVideoClick(pic) else onImageClick(index, pic)
                        }
                        if (pic.isVideo) {
                            Icon(
                                icon = Icons.SmartDisplay,
                                color = Theme.color.onContainer,
                                modifier = Modifier.size(Theme.size.image8).zIndex(2f)
                            )
                        }
                    }
                }
            }
        }
    ) { measurables, constraints ->
        val targetWidth = if (constraints.hasFixedWidth) constraints.maxWidth else constraints.constrainWidth(minPicWidthPx)

        if (picSize == 1) {
            // 单张
            val measurable = measurables.first()
            val pic = pics[0]

            val maxHeight = if (pic.isVideo) {
                if (constraints.hasBoundedHeight) constraints.maxHeight else Constraints.Infinity
            } else {
                if (constraints.hasBoundedHeight) minOf(targetWidth, constraints.maxHeight) else targetWidth
            }

            val placeable = measurable.measure(
                Constraints(minWidth = targetWidth, maxWidth = targetWidth, minHeight = 0, maxHeight = maxHeight)
            )

            val layoutHeight = constraints.constrainHeight(placeable.height)

            layout(targetWidth, layoutHeight) {
                placeable.placeRelative(0, 0)
            }
        }
        else {
            // 九宫格
            val [columns, rows, slots] = buildGridSpec(picSize)
            val safeWidth = targetWidth.coerceAtLeast(0)
            val effectiveSpacePx = if (columns <= 1) 0 else minOf(spacePx, safeWidth / (columns - 1))
            val horizontalSpace = (columns - 1) * effectiveSpacePx
            val availableWidth = (safeWidth - horizontalSpace).coerceAtLeast(0)
            val cellSize = (availableWidth / columns).coerceAtLeast(0)
            val contentHeight = (rows * cellSize + (rows - 1) * effectiveSpacePx).coerceAtLeast(0)
            val layoutHeight = constraints.constrainHeight(contentHeight)
            val placeables = measurables.mapIndexed { index, measurable ->
                (val columnSpan, val rowSpan) = slots[index]
                val slotWidth = (columnSpan * cellSize + (columnSpan - 1) * effectiveSpacePx).coerceAtLeast(0)
                val slotHeight = (rowSpan * cellSize + (rowSpan - 1) * effectiveSpacePx).coerceAtLeast(0)
                measurable.measure(Constraints.fixed(width = slotWidth, height = slotHeight))
            }

            layout(width = targetWidth, height = layoutHeight) {
                placeables.forEachIndexed { index, placeable ->
                    val [column, row] = slots[index]
                    val x = column * (cellSize + effectiveSpacePx)
                    val y = row * (cellSize + effectiveSpacePx)
                    placeable.placeRelative(x = x, y = y)
                }
            }
        }
    }
}