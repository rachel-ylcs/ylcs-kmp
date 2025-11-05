package love.yinlin.compose.graphics

import androidx.compose.runtime.Stable
import kotlinx.io.Sink
import kotlinx.io.Source
import love.yinlin.data.compose.ImageCropResult
import love.yinlin.data.compose.ImageQuality
import kotlin.math.*

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class ImmutableImage

expect class ImageOwner

@Stable
fun interface ImageOp {
    suspend fun process(@ImmutableImage owner: ImageOwner, quality: ImageQuality): ImageOwner?
}

@Stable
expect object ImageCompress : ImageOp {
    override suspend fun process(@ImmutableImage owner: ImageOwner, quality: ImageQuality): ImageOwner?
}

@Stable
expect class ImageCrop(rect: ImageCropResult): ImageOp {
    override suspend fun process(@ImmutableImage owner: ImageOwner, quality: ImageQuality): ImageOwner?
}

@Stable
class ImageProcessor(
    vararg items: ImageOp,
    private val quality: ImageQuality = ImageQuality.Medium
) {
    private val items = mutableListOf(*items)
    operator fun invoke(op: ImageOp): ImageProcessor = apply { items += op }
    suspend fun process(source: Source, sink: Sink): Boolean = imageProcess(source, sink, items, quality)
}

internal expect suspend fun imageProcess(source: Source, sink: Sink, items: List<ImageOp>, quality: ImageQuality): Boolean

@Stable
internal data class ScaleQualityInfo(
    val width: Int,
    val height: Int,
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
                    ScaleQualityInfo((width / scale).toInt(), (height / scale).toInt(), true)
                }
                else ScaleQualityInfo(width, height, false)
            }
            else { // 普通图
                if (minSize > MAX_SIZE_NORMAL) {
                    val scale = minSize / MAX_SIZE_NORMAL.toFloat()
                    ScaleQualityInfo((width / scale).toInt(), (height / scale).toInt(), true)
                }
                else ScaleQualityInfo(width, height, false)
            }
        }
    }
}