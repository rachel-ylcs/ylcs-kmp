package love.yinlin.compose.graphics

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import love.yinlin.data.compose.ImageCropResult
import love.yinlin.data.compose.ImageFormat
import love.yinlin.data.compose.ImageQuality
import love.yinlin.extension.catching
import love.yinlin.extension.catchingNull
import java.io.ByteArrayOutputStream

// ByteArray <-> ImageBitmap

private fun ImageFormat.encodedImageFormat(quality: ImageQuality): Bitmap.CompressFormat = when {
    this == ImageFormat.JPG -> Bitmap.CompressFormat.JPEG
    this == ImageFormat.PNG -> Bitmap.CompressFormat.PNG
    Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> Bitmap.CompressFormat.WEBP
    quality == ImageQuality.Full -> Bitmap.CompressFormat.WEBP_LOSSLESS
    else -> Bitmap.CompressFormat.WEBP_LOSSY
}

actual fun ImageBitmap.Companion.decode(data: ByteArray): ImageBitmap? = catchingNull {
    BitmapFactory.decodeByteArray(data, 0, data.size).asImageBitmap()
}

actual fun ImageBitmap.encode(format: ImageFormat, quality: ImageQuality): ByteArray? = catchingNull {
    val stream = ByteArrayOutputStream(this.width * this.height / 32)
    val bitmap = this.asAndroidBitmap()
    bitmap.compress(format.encodedImageFormat(quality), quality.value, stream)
    stream.toByteArray()
}

// Image process

actual class PlatformImage(internal var bitmap: Bitmap) {
    actual val width: Int get() = bitmap.width
    actual val height: Int get() = bitmap.height
    actual val hasAlpha: Boolean get() = bitmap.hasAlpha()
    actual companion object
}

actual fun PlatformImage.Companion.decode(data: ByteArray): PlatformImage? = catchingNull {
    PlatformImage(BitmapFactory.decodeByteArray(data, 0, data.size))
}

actual fun PlatformImage.encode(format: ImageFormat, quality: ImageQuality): ByteArray? = catchingNull {
    val stream = ByteArrayOutputStream(bitmap.width * bitmap.height / 32)
    bitmap.compress(format.encodedImageFormat(quality), quality.value, stream)
    stream.toByteArray()
}

actual fun PlatformImage.detachImageBitmap(): ImageBitmap = bitmap.asImageBitmap()

actual fun PlatformImage.copy(): PlatformImage? {
    return PlatformImage(bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false))
}

actual fun PlatformImage.crop(rect: ImageCropResult) = catching {
    val cropBitmap = Bitmap.createBitmap(
        bitmap,
        (rect.xPercent * bitmap.width).toInt(),
        (rect.yPercent * bitmap.height).toInt(),
        (rect.widthPercent * bitmap.width).toInt(),
        (rect.heightPercent * bitmap.height).toInt()
    )
    bitmap.recycle()
    bitmap = cropBitmap
}

actual fun PlatformImage.thumbnail(longImageThreshold: Float, maxSizeNormal: Int, minSizeLong: Int) = catching {
    val (thumbWidth, thumbHeight, scale) = calculateThumbnailScale(bitmap.width, bitmap.height, longImageThreshold, maxSizeNormal, minSizeLong)
    if (scale) {
        val thumbBitmap = Bitmap.createScaledBitmap(
            bitmap,
            thumbWidth,
            thumbHeight,
            true
        )
        bitmap.recycle()
        bitmap = thumbBitmap
    }
}