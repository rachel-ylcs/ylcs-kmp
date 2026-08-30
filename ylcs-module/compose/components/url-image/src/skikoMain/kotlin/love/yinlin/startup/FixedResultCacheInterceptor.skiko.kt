package love.yinlin.startup

import com.github.panpf.sketch.BitmapImage
import com.github.panpf.sketch.SketchImage
import com.github.panpf.sketch.asImage
import com.github.panpf.sketch.createBitmap
import com.github.panpf.sketch.decode.DecodeConfig
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.annotation.WorkerThread
import com.github.panpf.sketch.request.RequestContext
import com.github.panpf.sketch.source.DataSource
import com.github.panpf.sketch.source.toByteArray
import com.github.panpf.sketch.util.Size
import okio.BufferedSink
import org.jetbrains.skia.*
import org.jetbrains.skia.impl.use
import kotlin.math.ceil

@WorkerThread
actual fun compressFixed(image: SketchImage, sink: BufferedSink) {
    require(image is BitmapImage) { "Unsupported image type: ${image::class}" }
    val encodedData = Image.makeFromBitmap(image.bitmap).use {
        it.encodeToData(format = EncodedImageFormat.PNG, quality = 100)
    }
    encodedData?.use {
        sink.write(it.bytes)
    }
}

private fun calculateSampledBitmapSize(
    imageSize: Size,
    sampleSize: Int
): Size {
    val widthValue = imageSize.width / sampleSize.toDouble()
    val heightValue = imageSize.height / sampleSize.toDouble()
    val width: Int = ceil(widthValue).toInt()
    val height: Int = ceil(heightValue).toInt()
    return Size(width, height)
}

private fun Image.decode(config: DecodeConfig? = null): Bitmap {
    val sampleSize = config?.sampleSize ?: 1
    val bitmapSize = calculateSampledBitmapSize(
        imageSize = Size(width, height),
        sampleSize = sampleSize
    )
    val newColorType = config?.colorType ?: colorType
    val newColorSpace = config?.colorSpace ?: colorSpace
    val newImageInfo = org.jetbrains.skia.ImageInfo(
        width = bitmapSize.width,
        height = bitmapSize.height,
        colorType = newColorType,
        alphaType = alphaType,
        colorSpace = newColorSpace
    )
    val bitmap = createBitmap(newImageInfo)
    val canvas = Canvas(bitmap)
    canvas.drawImageRect(
        image = this,
        src = Rect.makeWH(width.toFloat(), height.toFloat()),
        dst = Rect.makeWH(bitmapSize.width.toFloat(), bitmapSize.height.toFloat())
    )
    return bitmap
}

@WorkerThread
actual fun decodeFixed(requestContext: RequestContext, imageInfo: ImageInfo, dataSource: DataSource): SketchImage {
    val bytes = dataSource.toByteArray()
    val skiaBitmap = Image.makeFromEncoded(bytes).use {
        val decodeConfig = DecodeConfig(
            request = requestContext.request,
            mimeType = imageInfo.mimeType,
            isOpaque = it.imageInfo.isOpaque
        )
        it.decode(decodeConfig)
    }
    return skiaBitmap.asImage()
}

actual fun supportImageFixed(image: SketchImage): Boolean = image is BitmapImage