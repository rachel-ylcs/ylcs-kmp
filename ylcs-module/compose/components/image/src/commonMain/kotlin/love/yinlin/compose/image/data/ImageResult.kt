package love.yinlin.compose.image.data

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.unit.IntSize

@Stable
internal class ImageResult(
    val image: ImageBitmap,
    val originalSize: IntSize,
    val origin: ImageOrigin,
    internal val animation: ImageAnimationData? = null,
) {
    val size: IntSize get() = IntSize(image.width, image.height)
    val isAnimated: Boolean get() = animation != null
    internal val byteCount: Long get() = image.width.toLong() * image.height * (if (image.config == ImageBitmapConfig.F16) 8L else 4L) + (animation?.bytes?.size ?: 0) + (animation?.durations?.size?.toLong() ?: 0L) * 4L

    internal fun fromMemory(): ImageResult = ImageResult(image, originalSize, ImageOrigin.Memory, animation)
}
