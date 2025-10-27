package love.yinlin.compose.ui.image

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.min
import androidx.compose.ui.zIndex
import love.yinlin.compose.*

@Composable
fun ReplaceableImage(
    uri: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    modifier: Modifier = Modifier,
    onReplace: () -> Unit,
    onDelete: () -> Unit
) {
    Box(modifier = modifier) {
        if (uri == null) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onReplace),
                contentAlignment = Alignment.Center
            ) {
                MiniIcon(
                    icon = Icons.Outlined.Add,
                    size = min(maxWidth, maxHeight) / 2f
                )
            }
        }
        else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ClickIcon(
                    icon = Icons.Outlined.Cancel,
                    color = MaterialTheme.colorScheme.error,
                    size = CustomTheme.size.mediumIcon,
                    modifier = Modifier.align(Alignment.TopEnd).zIndex(2f),
                    onClick = onDelete
                )
                WebImage(
                    uri = uri,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize().zIndex(1f),
                    onClick = onReplace
                )
            }
        }
    }
}