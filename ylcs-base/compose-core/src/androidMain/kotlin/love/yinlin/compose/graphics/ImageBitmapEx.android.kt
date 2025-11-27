package love.yinlin.compose.graphics

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import love.yinlin.data.compose.ImageFormat
import love.yinlin.data.compose.ImageQuality
import love.yinlin.extension.catchingNull
import java.io.ByteArrayOutputStream

val ImageQuality.webpFormat: Bitmap.CompressFormat get() = when {
    Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> Bitmap.CompressFormat.WEBP
    this == ImageQuality.Full -> Bitmap.CompressFormat.WEBP_LOSSLESS
    else -> Bitmap.CompressFormat.WEBP_LOSSY
}

fun ImageFormat.bitmapFormat(quality: ImageQuality): Bitmap.CompressFormat = when (this) {
    ImageFormat.JPG -> Bitmap.CompressFormat.JPEG
    ImageFormat.PNG -> Bitmap.CompressFormat.PNG
    else -> quality.webpFormat
}

actual fun ByteArray.decodeImage(): ImageBitmap? = BitmapFactory.decodeByteArray(this, 0, this.size)?.asImageBitmap()

actual fun ImageBitmap.encodeImage(format: ImageFormat, quality: ImageQuality): ByteArray? {
    var bitmap: Bitmap? = null
    val data = catchingNull {
        val stream = ByteArrayOutputStream()
        bitmap = this.asAndroidBitmap()
        bitmap.compress(format.bitmapFormat(quality), quality.value, stream)
        stream.toByteArray()
    }
    bitmap?.recycle()
    return data
}