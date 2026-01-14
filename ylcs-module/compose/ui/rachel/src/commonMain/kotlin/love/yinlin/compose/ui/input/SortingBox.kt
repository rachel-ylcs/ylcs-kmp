package love.yinlin.compose.ui.input

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastForEachIndexed
import love.yinlin.compose.collection.StableList
import love.yinlin.compose.ui.CustomTheme
import love.yinlin.compose.ui.image.ClickIcon
import love.yinlin.compose.ui.image.MiniIcon

@Composable
fun <T> SortingBox(
    items: StableList<T>,
    onMove: (Int, Int) -> Unit,
    title: (T) -> String,
    icon: ((T) -> ImageVector)? = null,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace),
    ) {
        items.fastForEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon?.let { MiniIcon(it(item), color) }
                Text(
                    text = title(item),
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(CustomTheme.padding.equalSpace)
                )
                ClickIcon(
                    icon = Icons.Outlined.ArrowUpward,
                    color = color,
                    enabled = index != 0,
                    onClick = { onMove(index, index - 1) }
                )
                ClickIcon(
                    icon = Icons.Outlined.ArrowDownward,
                    color = color,
                    enabled = index != items.lastIndex,
                    onClick = { onMove(index, index + 1) }
                )
            }
        }
    }
}