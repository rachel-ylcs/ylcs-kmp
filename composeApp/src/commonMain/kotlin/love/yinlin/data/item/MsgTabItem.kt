package love.yinlin.data.item

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diversity1
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

enum class MsgTabItem(
	val title: String,
	val icon: ImageVector
) {
	WEIBO("微博", Icons.Filled.Newspaper),
	CHAOHUA("超话", Icons.Filled.Diversity1),
	PICTURES("美图", Icons.Filled.PhotoLibrary)
}