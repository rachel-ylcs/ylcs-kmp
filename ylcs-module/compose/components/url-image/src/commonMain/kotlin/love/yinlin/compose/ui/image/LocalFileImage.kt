package love.yinlin.compose.ui.image

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.github.panpf.sketch.AsyncImage
import kotlinx.io.files.Path
import love.yinlin.compose.Theme
import love.yinlin.compose.data.ImageQuality
import love.yinlin.compose.ui.node.condition
import love.yinlin.extension.fileSize

@Composable
fun LocalFileImage(
    uri: String,
    vararg key: Any,
    modifier: Modifier = Modifier,
    circle: Boolean = false,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    alpha: Float = 1f,
    animated: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Box(modifier = modifier) {
        AsyncImage(
            uri = remember(uri, *key) { buildWebImageKeyUrl(uri, Path(uri).fileSize) },
            contentDescription = null,
            state = rememberWebImageState(ImageQuality.Full, background = null, animated = animated),
            alignment = alignment,
            contentScale = contentScale,
            filterQuality = ImageQuality.Full.filterQuality,
            alpha = alpha,
            modifier = Modifier
                .matchParentSize()
                .condition(circle) { clip(Theme.shape.circle) }
                .condition(onClick != null) { clickable(onClick = onClick) }
        )
    }
}