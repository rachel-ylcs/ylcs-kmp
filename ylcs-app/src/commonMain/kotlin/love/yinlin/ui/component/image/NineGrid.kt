package love.yinlin.ui.component.image

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import love.yinlin.common.Colors
import love.yinlin.data.common.Picture

@Composable
fun NineGrid(
	pics: List<Picture>,
	padding: Dp = 5.dp,
	modifier: Modifier = Modifier,
	onImageClick: (Int) -> Unit,
	onVideoClick: (Picture) -> Unit
) {
	val size = pics.size.coerceAtMost(9)
	if (size == 1) {
		val pic = pics[0]
		Box(
			modifier = modifier.height(200.dp),
			contentAlignment = Alignment.Center
		) {
			WebImage(
				uri = pic.image,
				modifier = Modifier.matchParentSize().zIndex(1f),
				onClick = {
					if (pic.isVideo) onVideoClick(pic)
					else onImageClick(0)
				}
			)
			if (pic.isVideo) {
				MiniIcon(
					icon = Icons.Outlined.SmartDisplay,
					size = 48.dp,
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
								WebImage(
									uri = pics[index].image,
									modifier = Modifier.size(squareSize),
									contentScale = ContentScale.Crop,
									onClick = { onImageClick(index) }
								)
							}
						}
					}
				}
			}
		}
	}
}