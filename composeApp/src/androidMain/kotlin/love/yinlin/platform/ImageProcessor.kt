package love.yinlin.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.geometry.Rect
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.asInputStream
import kotlinx.io.asOutputStream

actual object ImageProcessor {
    actual fun compress(source: Source, sink: Sink): Boolean = try {
        var result: Boolean
        val bitmap = BitmapFactory.decodeStream(source.asInputStream())
        val info = ScaleQualityInfo.calculate(bitmap.width, bitmap.height)
        if (info.scale) {
            val scaleBitmap = Bitmap.createScaledBitmap(bitmap, info.width, info.height, true)
            result = scaleBitmap.compress(Bitmap.CompressFormat.WEBP, info.quality.value, sink.asOutputStream())
            scaleBitmap.recycle()
        }
        else result = bitmap.compress(Bitmap.CompressFormat.WEBP, info.quality.value, sink.asOutputStream())
        bitmap.recycle()
        result
    } catch (_: Throwable) { false }

    actual fun crop(source: Source, sink: Sink, rect: Rect): Boolean = try {
        val result: Boolean
        val bitmap = BitmapFactory.decodeStream(source.asInputStream())
        val croppedBitmap = Bitmap.createBitmap(bitmap, rect.left.toInt(), rect.top.toInt(), rect.width.toInt(), rect.height.toInt())
        result = croppedBitmap.compress(Bitmap.CompressFormat.WEBP, 100, sink.asOutputStream())
        croppedBitmap.recycle()
        bitmap.recycle()
        result
    } catch (_: Throwable) { false }
}