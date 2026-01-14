package love.yinlin.compose.ui.image

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import love.yinlin.compose.Colors
import love.yinlin.compose.collection.StableList
import love.yinlin.compose.data.Picture
import love.yinlin.compose.ui.CustomTheme

@Composable
fun NineGrid(
    pics: StableList<Picture>,
    padding: Dp = CustomTheme.padding.littleSpace,
    modifier: Modifier = Modifier,
    onImageClick: (Int) -> Unit,
    onVideoClick: (Picture) -> Unit = {},
    content: @Composable (Modifier, Picture, ContentScale, () -> Unit) -> Unit
) {
    val size = pics.size.coerceAtMost(9)
    if (size == 1) {
        val pic = pics[0]
        Box(
            modifier = modifier.height(if (pic.isVideo) CustomTheme.size.extraImage else CustomTheme.size.cardWidth),
            contentAlignment = Alignment.Center
        ) {
            content(Modifier.matchParentSize().zIndex(1f), pic, ContentScale.Fit) {
                if (pic.isVideo) onVideoClick(pic)
                else onImageClick(0)
            }
            if (pic.isVideo) {
                MiniIcon(
                    icon = Icons.Outlined.SmartDisplay,
                    size = CustomTheme.size.image,
                    color = Colors.White,
                    modifier = Modifier.zIndex(2f)
                )
            }
        }
    }
    else {
        BoxWithConstraints(modifier = modifier) {
            val columnCount = if (size in 2 .. 4) 2 else 3
            val rowCount = (size + columnCount - 1) / columnCount
            val squareSize = (maxWidth - padding * (columnCount - 1)) / columnCount
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(padding)
            ) {
                repeat(rowCount) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(padding)
                    ) {
                        repeat(columnCount) { col ->
                            val index = row * columnCount + col
                            if (index < size) {
                                content(Modifier.size(squareSize), pics[index], ContentScale.Crop) {
                                    onImageClick(index)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}