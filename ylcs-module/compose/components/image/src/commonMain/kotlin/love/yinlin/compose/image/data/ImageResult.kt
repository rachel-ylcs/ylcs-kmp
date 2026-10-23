package love.yinlin.compose.image.data

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize

@Stable
internal class ImageResult(
    val image: ImageBitmap,
    val originalSize: IntSize,
    val origin: ImageOrigin,
    internal val animation: ImageAnimationData? = null,
) {
    val size: IntSize = IntSize(image.width, image.height)
    val isAnimated: Boolean get() = animation != null
    internal val byteCount: Long = image.allocationBytes() + (animation?.bytes?.size ?: 0) + (animation?.durations?.size?.toLong() ?: 0L) * 4L
    internal fun fromMemory(): ImageResult = ImageResult(image, originalSize, ImageOrigin.Memory, animation)
    internal fun forCache(): ImageResult = if (animation?.platform == null) this else ImageResult(image, originalSize, origin, animation.forCache())
}
