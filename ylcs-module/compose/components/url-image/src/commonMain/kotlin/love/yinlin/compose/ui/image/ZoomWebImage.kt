package love.yinlin.compose.ui.image

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.github.panpf.sketch.ability.progressIndicator
import com.github.panpf.zoomimage.SketchZoomAsyncImage
import com.github.panpf.zoomimage.rememberSketchZoomState
import love.yinlin.compose.data.ImageQuality

@Composable
fun ZoomWebImage(
    uri: String,
    key: Any? = null,
    modifier: Modifier = Modifier,
    quality: ImageQuality = ImageQuality.High,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    alpha: Float = 1f
) {
    Box(modifier = modifier) {
        val progressIndicator = rememberWebImageIndicator()
        val state = rememberWebImageState(quality, isCrossfade = false)
        val zoomState = rememberSketchZoomState()

        SketchZoomAsyncImage(
            uri = buildWebImageKeyUrl(uri, key),
            contentDescription = null,
            state = state,
            zoomState = zoomState,
            alignment = alignment,
            contentScale = contentScale,
            filterQuality = quality.filterQuality,
            alpha = alpha,
            scrollBar = null,
            modifier = Modifier.matchParentSize().progressIndicator(state, progressIndicator)
        )
    }
}