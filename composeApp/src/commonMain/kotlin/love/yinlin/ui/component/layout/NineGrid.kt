package love.yinlin.ui.component.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import love.yinlin.Colors
import love.yinlin.data.common.Picture
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.image.WebImage

@Composable
fun NineGrid(
	pics: List<Picture>,
	padding: Dp = 5.dp,
	modifier: Modifier = Modifier
) {
	val size = pics.size.coerceAtMost(9)
	if (size == 1) {
		val pic = pics.first()
		Box(
			modifier = modifier.height(200.dp),
			contentAlignment = Alignment.Center
		) {
			WebImage(
				uri = pic.image,
				modifier = Modifier.matchParentSize().zIndex(1f)
			)
			if (pic.isVideo) {
				MiniIcon(
					imageVector = Icons.Filled.SmartDisplay,
					size = 48.dp,
					color = Colors.Red4,
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
									contentScale = ContentScale.Crop
								)
							}
						}
					}
				}
			}
		}
	}
}