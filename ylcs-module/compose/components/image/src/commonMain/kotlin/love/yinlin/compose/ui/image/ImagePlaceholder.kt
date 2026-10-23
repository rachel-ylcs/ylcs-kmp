package love.yinlin.compose.ui.image

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.DpSize
import love.yinlin.compose.Theme
import love.yinlin.compose.image.data.ImageLoadStatus
import love.yinlin.compose.ui.icon.Icons

@Stable
data class ImagePlaceholder(
    val size: DpSize,
    val aspectRatio: Float? = null,
    val loading: @Composable BoxScope.(ImageState) -> Unit = DefaultBlock,
    val error: @Composable BoxScope.(ImageState) -> Unit = DefaultBlock,
    val empty: @Composable BoxScope.(ImageState) -> Unit = DefaultBlock,
) {
    companion object {
        private val DefaultBlock: @Composable BoxScope.(ImageState) -> Unit = { DefaultImageFeedback(it) }

        @Composable
        private fun BoxScope.DefaultImageFeedback(state: ImageState) {
            val viewport = state.size ?: return
            if (viewport.width <= 0 || viewport.height <= 0) return
            val viewportSize = with(LocalDensity.current) { DpSize(viewport.width.toDp(), viewport.height.toDp()) }
            val availableSize = minOf(viewportSize.width, viewportSize.height) * 0.75f
            val iconSize = minOf(Theme.size.icon, availableSize)

            Box(
                modifier = Modifier.matchParentSize().clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                when (state.status) {
                    ImageLoadStatus.Empty -> Icon(icon = Icons.Image, color = Theme.color.onSurfaceVariant, modifier = Modifier.size(iconSize))
                    ImageLoadStatus.Loading -> {
                        val feedbackSize = minOf(Theme.size.input7, availableSize)
                        val fraction = state.progress.fraction
                        val color = Theme.color.primaryContainer

                        val rotationMillis = Theme.animation.duration.v2.coerceAtLeast(1)
                        val phase = remember(state) { (state.loadingElapsedMillis() % rotationMillis).toFloat() / rotationMillis * 360f }
                        val rotation = if (fraction == null) rememberInfiniteTransition().animateFloat(phase, phase + 360f, infiniteRepeatable(tween(rotationMillis, easing = LinearEasing))) else null

                        Canvas(modifier = Modifier.size(feedbackSize)) {
                            val radius = size.minDimension / 2f
                            val stroke = radius * 0.2f
                            val arcSize = Size(radius * 2 - stroke, radius * 2 - stroke)

                            drawCircle(color.copy(alpha = 0.25f), radius - stroke / 2, style = Stroke(stroke))
                            drawArc(
                                color = color,
                                startAngle = rotation?.value ?: 270f,
                                sweepAngle = fraction?.times(360f) ?: 90f,
                                useCenter = false,
                                topLeft = Offset((size.width - arcSize.width) / 2, (size.height - arcSize.height) / 2),
                                size = arcSize,
                                style = Stroke(stroke, cap = StrokeCap.Round)
                            )
                        }

                        val iconColor = Theme.color.onSurfaceVariant
                        val iconFilter = remember(iconColor) { ColorFilter.tint(iconColor) }

                        Image(icon = Icons.Image, colorFilter = iconFilter, modifier = Modifier.size(feedbackSize * 0.4f))
                    }
                    ImageLoadStatus.Error -> {
                        if (state.canRetry) {
                            val viewConfiguration = LocalViewConfiguration.current
                            val touchTargetSize = DpSize(
                                minOf(viewConfiguration.minimumTouchTargetSize.width, viewportSize.width),
                                minOf(viewConfiguration.minimumTouchTargetSize.height, viewportSize.height),
                            )
                            val boundedConfiguration = remember(viewConfiguration, touchTargetSize) {
                                object : ViewConfiguration by viewConfiguration {
                                    override val minimumTouchTargetSize = touchTargetSize
                                }
                            }

                            CompositionLocalProvider(LocalViewConfiguration provides boundedConfiguration) {
                                Icon(icon = Icons.Refresh, color = Theme.color.warning, onClick = state::retry, modifier = Modifier.size(iconSize))
                            }
                        }
                        else {
                            Icon(icon = Icons.Error, color = Theme.color.error, modifier = Modifier.size(iconSize))
                        }
                    }
                    else -> { }
                }
            }
        }

        @Stable
        val Default: ImagePlaceholder @Composable get() {
            val size = DpSize(Theme.size.input7, Theme.size.input7)

            return remember(size) {
                ImagePlaceholder(
                    size = size,
                    aspectRatio = null,
                    loading = DefaultBlock,
                    error = DefaultBlock,
                    empty = DefaultBlock
                )
            }
        }
    }

    init {
        require(size.width.value.isFinite() && size.width.value > 0 && size.height.value.isFinite() && size.height.value > 0)
        require(aspectRatio == null || (aspectRatio.isFinite() && aspectRatio > 0f))
    }
}
