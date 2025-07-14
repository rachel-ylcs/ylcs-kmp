package love.yinlin.platform

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readByteArray
import org.jetbrains.skia.*
import org.jetbrains.skia.impl.use

private val ImageQuality.samplingMode: SamplingMode get() = when (this) {
    ImageQuality.Low -> SamplingMode.DEFAULT
    ImageQuality.Medium -> SamplingMode.LINEAR
    ImageQuality.High, ImageQuality.Full -> SamplingMode.MITCHELL
}

private fun Bitmap.Companion.create(width: Int, height: Int, colorInfo: ColorInfo): Bitmap = Bitmap().apply {
    allocPixels(ImageInfo(colorInfo, width, height))
}

private fun Canvas.drawBitmap(
    @ImmutableImage bitmap: Bitmap,
    src: Rect = Rect.makeWH(bitmap.width.toFloat(), bitmap.height.toFloat()),
    dst: Rect,
    samplingMode: SamplingMode = SamplingMode.LINEAR
) {
    Image.makeFromBitmap(bitmap).use { image ->
        this.drawImageRect(
            image = image,
            src = src,
            dst = dst,
            samplingMode = samplingMode,
            paint = null,
            strict = true
        )
    }
}

actual typealias ImageOwner = Bitmap

actual suspend fun imageProcess(source: Source, sink: Sink, items: List<ImageOp>, quality: ImageQuality): Boolean = Coroutines.cpu {
    var bitmap: Bitmap? = null
    try {
        val bytes = Coroutines.io { source.readByteArray() }
        bitmap = Image.makeFromEncoded(bytes).use { image ->
            Bitmap.makeFromImage(image)
        }
        bitmap.setImmutable()
        for (op in items) {
            val result = op.process(bitmap!!, quality)
            if (result != null) {
                if (!bitmap.isClosed) bitmap.close()
                result.setImmutable()
                bitmap = result
            }
        }
        Image.makeFromBitmap(bitmap).use { image ->
            image.encodeToData(EncodedImageFormat.WEBP, quality.value)?.use { data ->
                Coroutines.io { sink.write(data.bytes) }
            } != null
        }
    } catch (_: Throwable) {
        false
    } finally {
        if (bitmap != null && !bitmap.isClosed) bitmap.close()
    }
}

actual data object ImageCompress : ImageOp {
    actual override suspend fun process(@ImmutableImage owner: ImageOwner, quality: ImageQuality): ImageOwner? {
        val info = ScaleQualityInfo.calculate(owner.width, owner.height)
        if (info.scale) {
            val result = Bitmap.create(info.width, info.height, owner.colorInfo)
            Canvas(result).use { canvas ->
                canvas.drawBitmap(
                    bitmap = owner,
                    dst = Rect.makeWH(info.width.toFloat(), info.height.toFloat()),
                    samplingMode = quality.samplingMode
                )
            }
            return result
        }
        return null
    }
}

actual data class ImageCrop actual constructor(val rect: CropResult): ImageOp {
    actual override suspend fun process(@ImmutableImage owner: ImageOwner, quality: ImageQuality): ImageOwner? {
        val actualX = rect.xPercent * owner.width
        val actualY = rect.yPercent * owner.height
        val actualWidth = rect.widthPercent * owner.width
        val actualHeight = rect.heightPercent * owner.height
        val result = Bitmap.create(actualWidth.toInt(), actualHeight.toInt(), owner.colorInfo)
        Canvas(result).use { canvas ->
            canvas.drawBitmap(
                bitmap = owner,
                src = Rect(actualX, actualY, actualX + actualWidth, actualY + actualHeight),
                dst = Rect.makeWH(actualWidth, actualHeight),
                samplingMode = SamplingMode.MITCHELL
            )
        }
        return result
    }
}