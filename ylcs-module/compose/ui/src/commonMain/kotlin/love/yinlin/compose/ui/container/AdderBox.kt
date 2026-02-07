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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastForEachIndexed
import love.yinlin.compose.LocalColor
import love.yinlin.compose.Theme
import love.yinlin.compose.collection.StableList
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon

@Composable
private inline fun LookaheadScope.AdderBoxCell(
    size: Dp,
    clickModifier: Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(Theme.shape.v7)
            .background(Theme.color.backgroundVariant)
            .then(clickModifier)
            .animateBounds(this),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun <T> AdderBox(
    maxNum: Int,
    items: StableList<T>,
    modifier: Modifier = Modifier,
    size: Dp = Theme.size.cell9,
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
            val actualItems = remember(maxNum, items) { items.take(maxNum) }

            CompositionLocalProvider(LocalColor provides Theme.color.onBackground) {
                actualItems.fastForEachIndexed { index, item ->
                    AdderBoxCell(
                        size = size,
                        clickModifier = Modifier.combinedClickable(
                            onClick = { onReplace(index, item) },
                            onLongClick = { onDelete(index, item) }
                        )
                    ) {
                        content(index, item)
                    }
                }

                if (actualItems.size < maxNum) {
                    AdderBoxCell(
                        size = size,
                        clickModifier = Modifier.clickable(onClick = onAdd)
                    ) {
                        Icon(icon = Icons.Add)
                    }
                }
            }
        }
    }
}