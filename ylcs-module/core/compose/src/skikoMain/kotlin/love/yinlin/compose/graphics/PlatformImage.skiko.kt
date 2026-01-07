package love.yinlin.compose.graphics

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import love.yinlin.data.compose.ImageCropResult
import love.yinlin.data.compose.ImageFormat
import love.yinlin.data.compose.ImageQuality
import love.yinlin.extension.catching
import love.yinlin.extension.catchingNull
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.impl.use

// ByteArray <-> ImageBitmap

private val ImageFormat.encodedImageFormat: EncodedImageFormat get() = when (this) {
    ImageFormat.JPG -> EncodedImageFormat.JPEG
    ImageFormat.PNG -> EncodedImageFormat.PNG
    ImageFormat.WEBP -> EncodedImageFormat.WEBP
}

actual fun ImageBitmap.Companion.decode(data: ByteArray): ImageBitmap? = catchingNull {
    Image.makeFromEncoded(data).use { image ->
        Bitmap.makeFromImage(image)
    }.asComposeImageBitmap()
}

actual fun ImageBitmap.encode(format: ImageFormat, quality: ImageQuality): ByteArray? = catchingNull {
    Image.makeFromBitmap(this.asSkiaBitmap()).use { image ->
        image.encodeToData(format.encodedImageFormat, quality.value)?.use { it.bytes }
    }
}

// Image process

actual class PlatformImage(internal var image: Image) {
    actual val width: Int get() = image.width
    actual val height: Int get() = image.height
    actual val hasAlpha: Boolean get() = !image.isOpaque
    actual companion object
}

actual fun PlatformImage.Companion.decode(data: ByteArray): PlatformImage? = catchingNull {
    PlatformImage(Image.makeFromEncoded(data))
}

actual fun PlatformImage.encode(format: ImageFormat, quality: ImageQuality): ByteArray? = catchingNull {
    image.encodeToData(format.encodedImageFormat, quality.value)?.use { it.bytes }
}

actual fun PlatformImage.detachImageBitmap(): ImageBitmap {
    val bitmap = Bitmap.makeFromImage(image)
    bitmap.setImmutable()
    image.close()
    return bitmap.asComposeImageBitmap()
}

actual fun PlatformImage.copy(): PlatformImage? = catchingNull {
    val bitmap = Bitmap.makeFromImage(image)
    bitmap.setImmutable()
    PlatformImage(Image.makeFromBitmap(bitmap))
}

actual fun PlatformImage.crop(rect: ImageCropResult) = catching {
    val actualX = rect.xPercent * image.width
    val actualY = rect.yPercent * image.height
    val actualWidth = rect.widthPercent * image.width
    val actualHeight = rect.heightPercent * image.height
    val cropBitmap = Bitmap()
    cropBitmap.allocPixels(ImageInfo(image.colorInfo, actualWidth.toInt(), actualHeight.toInt()))
    Canvas(cropBitmap).use { canvas ->
        canvas.drawImageRect(
            image = image,
            src = Rect(actualX, actualY, actualX + actualWidth, actualY + actualHeight),
            dst = Rect.makeWH(actualWidth, actualHeight),
            samplingMode = SamplingMode.MITCHELL,
            paint = Paint().apply { isAntiAlias = true },
            strict = true
        )
    }
    image.close()
    cropBitmap.setImmutable()
    image = Image.makeFromBitmap(cropBitmap)
}

actual fun PlatformImage.thumbnail(longImageThreshold: Float, maxSizeNormal: Int, minSizeLong: Int) = catching {
    val (thumbWidth, thumbHeight, scale) = calculateThumbnailScale(image.width, image.height, longImageThreshold, maxSizeNormal, minSizeLong)
    if (scale) {
        val thumbBitmap = Bitmap()
        thumbBitmap.allocPixels(ImageInfo(image.colorInfo, thumbWidth, thumbHeight))
        Canvas(thumbBitmap).use { canvas ->
            canvas.drawImageRect(
                image = image,
                src = Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
                dst = Rect.makeWH(thumbWidth.toFloat(), thumbHeight.toFloat()),
                samplingMode = SamplingMode.LINEAR,
                paint = Paint().apply { isAntiAlias = true },
                strict = true
            )
        }
        image.close()
        thumbBitmap.setImmutable()
        image = Image.makeFromBitmap(thumbBitmap)
    }
}