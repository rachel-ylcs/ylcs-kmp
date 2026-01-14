package love.yinlin.compose.graphics

import androidx.compose.ui.graphics.ImageBitmap
import love.yinlin.compose.data.ImageCropResult
import love.yinlin.compose.data.ImageFormat
import love.yinlin.compose.data.ImageQuality
import kotlin.math.max
import kotlin.math.min

// ByteArray <-> ImageBitmap

expect fun ImageBitmap.Companion.decode(data: ByteArray): ImageBitmap?
expect fun ImageBitmap.encode(format: ImageFormat = ImageFormat.WEBP, quality: ImageQuality = ImageQuality.Full): ByteArray?

// Image process

expect class PlatformImage {
    val width: Int
    val height: Int
    val hasAlpha: Boolean

    companion object
}

expect fun PlatformImage.Companion.decode(data: ByteArray): PlatformImage?
expect fun PlatformImage.encode(format: ImageFormat = ImageFormat.WEBP, quality: ImageQuality = ImageQuality.Full): ByteArray?
expect fun PlatformImage.detachImageBitmap(): ImageBitmap
expect fun PlatformImage.copy(): PlatformImage?


expect fun PlatformImage.crop(rect: ImageCropResult)

internal fun calculateThumbnailScale(width: Int, height: Int, longImageThreshold: Float, maxSizeNormal: Int, minSizeLong: Int): Triple<Int, Int, Boolean> {
    val aspectRatio = width / height.toFloat()
    val minSize = min(width, height)
    val maxSize = max(width, height)
    return if (aspectRatio < 1 / longImageThreshold || aspectRatio > longImageThreshold) { // 长图
        if (maxSize > minSizeLong) { // 需要缩放
            val scale = maxSize / minSizeLong.toFloat()
            Triple((width / scale).toInt(), (height / scale).toInt(), true)
        }
        else Triple(width, height, false)
    }
    else { // 普通图
        if (minSize > maxSizeNormal) {
            val scale = minSize / maxSizeNormal.toFloat()
            Triple((width / scale).toInt(), (height / scale).toInt(), true)
        }
        else Triple(width, height, false)
    }
}

expect fun PlatformImage.thumbnail(longImageThreshold: Float = 3.33333f, maxSizeNormal: Int = 1080, minSizeLong: Int = 3000)