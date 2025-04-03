package love.yinlin.platform

import androidx.compose.ui.graphics.toSkiaRect
import com.github.panpf.sketch.createBitmap
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readByteArray
import org.jetbrains.skia.*
import org.jetbrains.skia.impl.use

private typealias ComposeRect = androidx.compose.ui.geometry.Rect

val ImageQuality.samplingMode: SamplingMode get() = when (this) {
    ImageQuality.Low -> SamplingMode.DEFAULT
    ImageQuality.Medium -> SamplingMode.LINEAR
    ImageQuality.High, ImageQuality.Full -> SamplingMode.MITCHELL
}

actual object ImageProcessor {
    actual fun compress(source: Source, sink: Sink): Boolean = try {
        Image.makeFromEncoded(source.readByteArray()).use { image ->
            val info = ScaleQualityInfo.calculate(image.width, image.height)
            if (info.scale) createBitmap(image.imageInfo.withWidthHeight(info.width, info.height)).use { bitmap ->
                Canvas(bitmap).use { canvas ->
                    canvas.drawImageRect(
                        image = image,
                        src = Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
                        dst = Rect.makeWH(info.width.toFloat(), info.height.toFloat()),
                        samplingMode = info.quality.samplingMode,
                        paint = null,
                        strict = true
                    )
                }
                Image.makeFromBitmap(bitmap).use { scaleImage ->
                    scaleImage.encodeToData(EncodedImageFormat.WEBP, info.quality.value)?.use { data ->
                        sink.write(data.bytes)
                    } != null
                }
            }
            else image.encodeToData(EncodedImageFormat.WEBP, info.quality.value)?.use { data ->
                sink.write(data.bytes)
            } != null
        }
    } catch (_: Throwable) { false }

    actual fun crop(source: Source, sink: Sink, rect: ComposeRect): Boolean = try {
        Image.makeFromEncoded(source.readByteArray()).use { image ->
            createBitmap(image.imageInfo.withWidthHeight(width = rect.width.toInt(), height = rect.height.toInt())).use { bitmap ->
                Canvas(bitmap).use { canvas ->
                    canvas.drawImageRect(
                        image = image,
                        src = rect.toSkiaRect(),
                        dst = Rect.makeWH(rect.width, rect.height),
                        samplingMode = SamplingMode.MITCHELL,
                        paint = null,
                        strict = true
                    )
                }
                Image.makeFromBitmap(bitmap).use { croppedImage ->
                    croppedImage.encodeToData(EncodedImageFormat.WEBP, ImageQuality.Full.value)?.use { data ->
                        sink.write(data.bytes)
                    } != null
                }
            }
        }
    } catch (_: Throwable) { false }
}