package love.yinlin.compose.ui.image

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.roundToIntSize
import com.github.panpf.sketch.AsyncImage
import com.github.panpf.sketch.AsyncImageState
import com.github.panpf.sketch.LocalPlatformContext
import com.github.panpf.sketch.SingletonSketch
import com.github.panpf.sketch.ability.progressIndicator
import com.github.panpf.sketch.painter.internal.AbsProgressPainter
import com.github.panpf.sketch.rememberAsyncImageState
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.request.disallowAnimatedImage
import com.github.panpf.sketch.request.pauseLoadWhenScrolling
import com.github.panpf.sketch.state.ColorPainterStateImage
import love.yinlin.compose.CustomTheme
import love.yinlin.compose.LocalAnimationSpeed
import love.yinlin.compose.component.url_image.resources.Res
import love.yinlin.compose.component.url_image.resources.placeholder_pic
import love.yinlin.data.compose.ImageQuality
import love.yinlin.compose.roundToIntOffset
import love.yinlin.compose.ui.node.condition
import org.jetbrains.compose.resources.imageResource

@Composable
internal fun rememberWebImageKeyUrl(uri: String, key: Any? = null): String = remember(uri, key) {
    if (key == null) uri
    else if (uri.contains("?")) "$uri&_cacheKey=$key"
    else "$uri?_cacheKey=$key"
}

@Composable
internal fun rememberWebImageState(
    quality: ImageQuality,
    background: Color? = MaterialTheme.colorScheme.scrim,
    isCrossfade: Boolean = true,
    animated: Boolean = true,
): AsyncImageState {
    val context = LocalPlatformContext.current
    val options = remember(quality, background, isCrossfade, animated) {
        ImageOptions.Builder().apply {
            sizeMultiplier(quality.sizeMultiplier)
            disallowAnimatedImage(!animated)
            pauseLoadWhenScrolling(true)
            background?.let { placeholder(ColorPainterStateImage(it)) }
            if (isCrossfade) crossfade()
        }.merge(SingletonSketch.get(context).globalImageOptions).build()
    }
    return rememberAsyncImageState(options)
}

@Stable
internal class WebImageIndicator(
    density: Density,
    animationSpeed: Int,
    private val indicatorSize: Dp,
    private val indicatorColor: Color,
    private val indicatorImage: ImageBitmap
) : AbsProgressPainter(
    hiddenWhenIndeterminate = false,
    hiddenWhenCompleted = true,
    stepAnimationDuration = animationSpeed
) {
    override val intrinsicSize: Size = with(density) { Size(indicatorSize.toPx(), indicatorSize.toPx()) }

    override fun DrawScope.drawProgress(drawProgress: Float) {
        val widthRadius = size.width / 2f
        val heightRadius = size.height / 2f
        val radius = widthRadius.coerceAtMost(heightRadius)
        val center = Offset(widthRadius, heightRadius)
        val ringWidth = (indicatorSize * 0.1f).toPx()
        drawImage(
            image = indicatorImage,
            dstOffset = Offset(x = intrinsicSize.width / 4, y = intrinsicSize.height / 4).roundToIntOffset(),
            dstSize = Size(radius, radius).roundToIntSize()
        )
        drawCircle(
            color = indicatorColor.copy(alpha = 0.25f),
            radius = radius,
            center = center,
            style = Stroke(ringWidth)
        )
        val sweepAngle = drawProgress * 360f
        drawArc(
            color = indicatorColor,
            startAngle = 270f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(widthRadius - radius, heightRadius - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(ringWidth, cap = StrokeCap.Round),
        )
    }
}

@Composable
internal fun rememberWebImageIndicator(): WebImageIndicator {
    val density = LocalDensity.current
    val animationSpeed = LocalAnimationSpeed.current
    val size = CustomTheme.size.mediumInput
    val color = MaterialTheme.colorScheme.primaryContainer
    val image = imageResource(Res.drawable.placeholder_pic)
    return remember(density, size, color, image) {
        WebImageIndicator(density, animationSpeed, size, color, image)
    }
}

@Composable
internal fun WebImage(
    uri: String,
    key: Any? = null,
    modifier: Modifier = Modifier,
    circle: Boolean = false,
    quality: ImageQuality = ImageQuality.Medium,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    alpha: Float = 1f,
    state: AsyncImageState,
    onClick: (() -> Unit)? = null
) {
    Box(modifier = modifier) {
        val progressIndicator = rememberWebImageIndicator()

        AsyncImage(
            uri = rememberWebImageKeyUrl(uri, key),
            contentDescription = null,
            state = state,
            alignment = alignment,
            contentScale = contentScale,
            filterQuality = quality.filterQuality,
            alpha = alpha,
            modifier = Modifier.matchParentSize().condition(circle) { clip(CircleShape) }
                .condition(onClick != null) { clickable(onClick = onClick ?: {}) }
                .progressIndicator(state, progressIndicator)
        )
    }
}

@Composable
fun WebImage(
    uri: String,
    key: Any? = null,
    modifier: Modifier = Modifier,
    circle: Boolean = false,
    quality: ImageQuality = ImageQuality.Medium,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    alpha: Float = 1f,
    animated: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    WebImage(
        uri = uri,
        key = key,
        modifier = modifier,
        circle = circle,
        quality = quality,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        state = rememberWebImageState(quality, animated = animated),
        onClick = onClick
    )
}