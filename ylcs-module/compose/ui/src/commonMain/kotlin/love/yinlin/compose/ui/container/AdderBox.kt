package love.yinlin.compose.ui.container

import androidx.compose.animation.animateBounds
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.Dp
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import kotlin.math.min

@Composable
private inline fun LookaheadScope.AdderBoxCell(
    size: Dp,
    shape: Shape,
    clickModifier: Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .animateBounds(this)
            .size(size)
            .clip(shape)
            .background(Theme.color.backgroundVariant)
            .then(clickModifier),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun <T> AdderBox(
    maxNum: Int,
    items: List<T>,
    modifier: Modifier = Modifier,
    size: Dp = Theme.size.cell9,
    shape: Shape = Theme.shape.v7,
    onAdd: () -> Unit = {},
    onReplace: (Int, T) -> Unit = { _, _ -> },
    onDelete: (Int, T) -> Unit = { _, _ -> },
    content: @Composable (Int, T) -> Unit
) {
    LookaheadScope {
        val space = size / 20f

        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(space),
            verticalArrangement = Arrangement.spacedBy(space)
        ) {
            BackgroundContainer {
                val itemNum = items.size

                for (index in 0 ..< min(itemNum, maxNum))  {
                    val item = items[index]
                    AdderBoxCell(
                        size = size,
                        shape = shape,
                        clickModifier = Modifier.combinedClickable(
                            onClick = { onReplace(index, item) },
                            onLongClick = { onDelete(index, item) }
                        )
                    ) {
                        content(index, item)
                    }
                }

                if (itemNum < maxNum) {
                    AdderBoxCell(
                        size = size,
                        shape = shape,
                        clickModifier = Modifier.clickable(onClick = onAdd)
                    ) {
                        Icon(icon = Icons.Add)
                    }
                }
            }
        }
    }
}