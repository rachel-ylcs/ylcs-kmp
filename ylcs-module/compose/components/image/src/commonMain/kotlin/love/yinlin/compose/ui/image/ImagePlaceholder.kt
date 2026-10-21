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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import love.yinlin.compose.Theme
import love.yinlin.compose.image.data.ImageLoadStatus
import love.yinlin.compose.ui.icon.Icons

/**
 * size 是无显式尺寸时的保留空间；单边确定时按 aspectRatio（或 size 比例）计算另一边。
 * 容器从占位到成功保持尺寸。外部 modifier 的固定尺寸和父约束优先，ContentScale 决定图像绘制。
 */
@Stable
data class ImagePlaceholder(
    val size: DpSize,
    val aspectRatio: Float? = null,
    val background: Color? = null,
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
            Box(Modifier.matchParentSize().clipToBounds(), contentAlignment = Alignment.Center) {
                when (state.status) {
                    ImageLoadStatus.Empty -> Icon(Icons.Image, color = Theme.color.onSurfaceVariant, tip = "暂无图片",
                        modifier = Modifier.size(iconSize))
                    ImageLoadStatus.Loading -> {
                        val feedbackSize = minOf(Theme.size.input7, availableSize)
                        val fraction = state.progress.fraction
                        val color = Theme.color.primaryContainer
                        val rotation by rememberInfiniteTransition().animateFloat(0f, 360f,
                            infiniteRepeatable(tween(Theme.animation.duration.v2, easing = LinearEasing)))
                        Canvas(Modifier.size(feedbackSize).semantics {
                            progressBarRangeInfo = fraction?.let { ProgressBarRangeInfo(it, 0f .. 1f) } ?: ProgressBarRangeInfo.Indeterminate
                        }) {
                            val radius = size.minDimension / 2f
                            val stroke = radius * 0.2f
                            drawCircle(color.copy(alpha = 0.25f), radius - stroke / 2, style = Stroke(stroke))
                            val arcSize = Size(radius * 2 - stroke, radius * 2 - stroke)
                            drawArc(color, if (fraction == null) rotation else 270f, fraction?.times(360f) ?: 90f, false,
                                topLeft = Offset((size.width - arcSize.width) / 2, (size.height - arcSize.height) / 2),
                                size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                        }
                        Icon(Icons.Image, color = Theme.color.onSurfaceVariant, modifier = Modifier.size(feedbackSize * 0.4f))
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
                                Icon(Icons.Refresh, color = Theme.color.warning, tip = "图片读取失败，点击重试",
                                    onClick = state::retry, modifier = Modifier.size(iconSize))
                            }
                        }
                        else Icon(Icons.Error, color = Theme.color.error, tip = "图片数据无法解码", modifier = Modifier.size(iconSize))
                    }
                    else -> { }
                }
            }
        }

        @Stable
        val Default: ImagePlaceholder @Composable get() {
            val size = DpSize(Theme.size.input7, Theme.size.input7)
            val background = Theme.color.scrim

            return remember(size, background) {
                ImagePlaceholder(
                    size = size,
                    aspectRatio = null,
                    background = background,
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
