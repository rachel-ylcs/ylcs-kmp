package love.yinlin.compose.ui.image

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.github.panpf.sketch.AsyncImage
import com.github.panpf.sketch.AsyncImageState
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.compose.data.ImageQuality
import love.yinlin.compose.ui.node.condition

@Composable
fun LocalFileImage(
    path: () -> Path,
    vararg key: Any,
    modifier: Modifier = Modifier,
    circle: Boolean = false,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = 1f,
    animated: Boolean = true,
    state: AsyncImageState = rememberWebImageState(ImageQuality.Full, background = null, animated = animated),
    onClick: (() -> Unit)? = null
) {
    val baseUri = remember(*key) { path().toString() }
    val baseKey = remember(*key) { SystemFileSystem.metadataOrNull(path())?.size ?: 0L }
    Box(modifier = modifier) {
        AsyncImage(
            uri = rememberWebImageKeyUrl(baseUri, baseKey),
            contentDescription = null,
            state = state,
            alignment = Alignment.Center,
            contentScale = contentScale,
            filterQuality = ImageQuality.Full.filterQuality,
            alpha = alpha,
            modifier = Modifier.matchParentSize().condition(circle) { clip(CircleShape) }
                .condition(onClick != null) { clickable(onClick = onClick ?: {}) }
        )
    }
}