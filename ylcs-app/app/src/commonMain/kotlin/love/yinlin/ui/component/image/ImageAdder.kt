package love.yinlin.ui.component.image

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import love.yinlin.common.ThemeValue
import love.yinlin.compose.rememberDerivedState
import love.yinlin.data.common.Picture

@Composable
fun ImageAdder(
    maxNum: Int,
    pics: List<Picture>,
    size: Dp,
    space: Dp = ThemeValue.Padding.EqualSpace,
    modifier: Modifier = Modifier,
    onAdd: () -> Unit,
    onDelete: (Int) -> Unit,
    onClick: (Int) -> Unit
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space),
        verticalArrangement = Arrangement.spacedBy(space)
    ) {
        val actualPics by rememberDerivedState(pics, maxNum) { pics.take(maxNum) }

        actualPics.fastForEachIndexed { index, pic ->
            Box(
                modifier = Modifier.size(size),
                contentAlignment = Alignment.TopEnd
            ) {
                ClickIcon(
                    icon = Icons.Outlined.Cancel,
                    color = MaterialTheme.colorScheme.error,
                    size = size / 3.5f,
                    modifier = Modifier.zIndex(2f),
                    onClick = { onDelete(index) }
                )
                WebImage(
                    uri = pic.image,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().zIndex(1f),
                    onClick = { onClick(index) }
                )
            }
        }
        if (actualPics.size < maxNum) {
            Box(
                modifier = Modifier.size(size)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center
            ) {
                MiniIcon(
                    icon = Icons.Outlined.Add,
                    size = size / 2
                )
            }
        }
    }
}