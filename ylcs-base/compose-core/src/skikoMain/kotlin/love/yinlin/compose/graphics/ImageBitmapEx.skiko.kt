package love.yinlin.compose.graphics

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import love.yinlin.data.compose.ImageFormat
import love.yinlin.data.compose.ImageQuality
import love.yinlin.extension.catchingNull
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.impl.use

val ImageFormat.encodedImageFormat: EncodedImageFormat get() = when (this) {
    ImageFormat.JPG -> EncodedImageFormat.JPEG
    ImageFormat.PNG -> EncodedImageFormat.PNG
    ImageFormat.WEBP -> EncodedImageFormat.WEBP
}

actual fun ByteArray.decodeImage(): ImageBitmap? = catchingNull {
    Image.makeFromEncoded(this).toComposeImageBitmap()
}

actual fun ImageBitmap.encodeImage(format: ImageFormat, quality: ImageQuality): ByteArray? {
    var bitmap: Bitmap? = null
    val data = catchingNull {
        bitmap = this.asSkiaBitmap()
        Image.makeFromBitmap(bitmap).use { image ->
            image.encodeToData(format.encodedImageFormat, quality.value)?.use { it.bytes }
        }
    }
    if (bitmap != null && !bitmap.isClosed) bitmap.close()
    return data
}