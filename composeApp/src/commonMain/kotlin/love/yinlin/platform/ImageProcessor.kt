package love.yinlin.platform

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlin.math.max
import kotlin.math.min

enum class ImageQuality {
    Low, Medium, High, Full;

    val value: Int get() = when (this) {
        Low -> 60
        Medium -> 80
        High -> 90
        Full -> 100
    }

    val sizeMultiplier: Float get() = when (this) {
        Low -> 1f
        Medium -> 2f
        High -> 3f
        Full -> 4f
    }

    val filterQuality: FilterQuality get() = when (this) {
        Low -> FilterQuality.Low
        Medium -> FilterQuality.Medium
        High, Full -> FilterQuality.High
    }
}

internal data class ScaleQualityInfo(
    val width: Int,
    val height: Int,
    val quality: ImageQuality,
    val scale: Boolean
) {
    companion object {
        private const val LONG_ASPECT_RATIO = 3.33333f
        private const val MAX_SIZE_NORMAL = 1080
        private const val MIN_SIZE_LONG = 3000

        fun calculate(width: Int, height: Int): ScaleQualityInfo {
            require(width > 0 && height > 0)
            val aspectRatio = width / height.toFloat()
            val minSize = min(width, height)
            val maxSize = max(width, height)
            return if (aspectRatio < 1 / LONG_ASPECT_RATIO || aspectRatio > LONG_ASPECT_RATIO) { // 长图
                if (maxSize > MIN_SIZE_LONG) { // 需要缩放
                    val scale = maxSize / MIN_SIZE_LONG.toFloat()
                    ScaleQualityInfo((width / scale).toInt(), (height / scale).toInt(), ImageQuality.High, true)
                }
                else ScaleQualityInfo(width, height, ImageQuality.Medium, false)
            }
            else { // 普通图
                if (minSize > MAX_SIZE_NORMAL) {
                    val scale = minSize / MAX_SIZE_NORMAL.toFloat()
                    ScaleQualityInfo((width / scale).toInt(), (height / scale).toInt(), ImageQuality.Medium, true)
                }
                else ScaleQualityInfo(width, height, ImageQuality.Medium, false)
            }
        }
    }
}

expect object ImageProcessor {
    fun compress(source: Source, sink: Sink): Boolean
    fun crop(source: Source, sink: Sink, rect: Rect): Boolean
}