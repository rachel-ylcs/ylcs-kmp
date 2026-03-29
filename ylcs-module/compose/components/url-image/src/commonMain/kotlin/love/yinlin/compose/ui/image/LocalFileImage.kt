package love.yinlin.compose.ui.image

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.github.panpf.sketch.AsyncImage
import love.yinlin.compose.Theme
import love.yinlin.compose.data.ImageQuality
import love.yinlin.compose.extension.rememberNull
import love.yinlin.compose.ui.node.condition
import love.yinlin.fs.File

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
    var actualUri: String? by rememberNull()

    LaunchedEffect(uri, *key) {
        actualUri = buildWebImageKeyUrl(uri, File(uri).fileSize())
    }

    Box(modifier = modifier) {
        actualUri?.let {
            AsyncImage(
                uri = it,
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
}