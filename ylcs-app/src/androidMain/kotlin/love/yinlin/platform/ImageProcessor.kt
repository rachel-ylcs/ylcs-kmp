package love.yinlin.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.asInputStream
import kotlinx.io.asOutputStream
import androidx.core.graphics.scale

actual typealias ImageOwner = Bitmap

actual suspend fun imageProcess(source: Source, sink: Sink, items: List<ImageOp>, quality: ImageQuality): Boolean = Coroutines.cpu {
    var bitmap: Bitmap? = null
    try {
        bitmap = Coroutines.io { BitmapFactory.decodeStream(source.asInputStream()) }
        for (op in items) {
            val result = op.process(bitmap!!, quality)
            if (result != null) {
                bitmap.recycle()
                bitmap = result
            }
        }
        Coroutines.io {
            bitmap.compress(Bitmap.CompressFormat.WEBP, quality.value, sink.asOutputStream())
        }
        true
    } catch (_: Throwable) {
        false
    } finally {
        bitmap?.recycle()
    }
}

actual data object ImageCompress : ImageOp {
    actual override suspend fun process(@ImmutableImage owner: ImageOwner, quality: ImageQuality): ImageOwner? {
        val info = ScaleQualityInfo.calculate(owner.width, owner.height)
        return if (info.scale) owner.scale(info.width, info.height) else null
    }
}

actual data class ImageCrop actual constructor(val rect: CropResult): ImageOp {
    actual override suspend fun process(@ImmutableImage owner: ImageOwner, quality: ImageQuality): ImageOwner? {
        return Bitmap.createBitmap(
            owner,
            (rect.xPercent * owner.width).toInt(),
            (rect.yPercent * owner.height).toInt(),
            (rect.widthPercent * owner.width).toInt(),
            (rect.heightPercent * owner.height).toInt()
        )
    }
}