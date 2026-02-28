package love.yinlin.compose.ui.image

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.data.compose.Picture
import kotlin.math.min

@Composable
fun NineGrid(
    pics: List<Picture>,
    modifier: Modifier = Modifier,
    space: Dp = Theme.padding.g3,
    onImageClick: (Int, Picture) -> Unit = { _, _ -> },
    onVideoClick: (Picture) -> Unit = {},
    content: @Composable (Boolean, Picture, () -> Unit) -> Unit
) {
    // 无图跳过
    val picSize = pics.size.coerceAtMost(9)
    if (picSize == 0) return

    // 默认图片最小宽度
    val density = LocalDensity.current
    val minPicWidthPx = with(density) { Theme.size.image2.toPx().toInt() }
    val paddingPx = with(density) { space.toPx().toInt() }

    Layout(
        modifier = modifier.clipToBounds(),
        content = {
            val isSingle = picSize == 1
            for (index in 0 ..< picSize) {
                val pic = pics[index]
                val isVideo = pic.isVideo

                Box(contentAlignment = Alignment.Center) {
                    content(isSingle, pic) {
                        if (isVideo) onVideoClick(pic) else onImageClick(index, pic)
                    }
                    if (isVideo) {
                        Icon(
                            icon = Icons.SmartDisplay,
                            color = Theme.color.onContainer,
                            modifier = Modifier.size(Theme.size.image8).zIndex(2f)
                        )
                    }
                }
            }
        }
    ) { measurables, constraints ->
        if (picSize == 1) {
            // 单张
            val measurable = measurables.first()
            val pic = pics[0]

            // 确定宽度
            val targetWidth = if (constraints.hasFixedWidth) constraints.maxWidth else if (constraints.hasBoundedWidth) min(constraints.maxWidth, minPicWidthPx) else minPicWidthPx

            // 测量
            val placeable = measurable.measure(Constraints(
                minWidth = targetWidth,
                maxWidth = targetWidth,
                minHeight = 0,
                maxHeight = Constraints.Infinity
            ))

            val rawHeight = placeable.height
            val layoutHeight = if (pic.isVideo) rawHeight else min(rawHeight, targetWidth)

            layout(targetWidth, layoutHeight) {
                placeable.placeRelative(0, 0)
            }
        }
        else {
            // 九宫格
            val columns = if (picSize <= 4) 2 else 3
            // 计算行数
            val rows = (picSize + columns - 1) / columns
            // 确定总宽度
            val totalWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else minPicWidthPx
            // 计算方格尺寸
            val itemSize = ((totalWidth - (columns - 1) * paddingPx) / columns).coerceAtLeast(0)

            // 测量
            val itemConstraints = Constraints.fixed(itemSize, itemSize)
            val placeables = measurables.map { it.measure(itemConstraints) }

            // 计算总高度
            val totalHeight = rows * itemSize + (rows - 1) * paddingPx

            layout(totalWidth, totalHeight) {
                placeables.forEachIndexed { index, placeable ->
                    placeable.placeRelative(
                        x = (index % columns) * (itemSize + paddingPx),
                        y = (index / columns) * (itemSize + paddingPx)
                    )
                }
            }
        }
    }
}