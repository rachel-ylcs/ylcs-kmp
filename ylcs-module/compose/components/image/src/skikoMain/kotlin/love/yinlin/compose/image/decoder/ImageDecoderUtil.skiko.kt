package love.yinlin.compose.image.decoder

import androidx.compose.ui.unit.IntSize
import love.yinlin.compose.image.ImageManager
import love.yinlin.compose.image.data.ImageDecodeOptions
import love.yinlin.compose.image.data.PlatformImageAnimation
import love.yinlin.extension.catchingError
import love.yinlin.extension.cleaning
import love.yinlin.io.ByteArrayIO
import org.jetbrains.skia.*

internal actual fun ImageDecoder.platformAnimation(): PlatformImageAnimation? = null

internal actual fun openPlatformImageDecoder(bytes: ByteArray, request: ImageDecodeOptions): ImageDecoder {
    if (!request.animated) openStaticBitmapDecoder(bytes, request)?.let { return it }

    val maxDecodePixels = ImageManager.configuration.maxDecodePixels

    // 非 sRGB 或平台不支持的直接解码路径沿用 Image 的色彩和方向处理
    if (!request.animated) {
        val image = Image.makeFromEncoded(bytes)
        var transferred = false

        return cleaning({
            val original = IntSize(image.width, image.height)
            val size = request.sampledSize(original)
            val decoder = SkiaImageDecoder(null, null, image, original, size, false, sampling = request.samplingMode)
            transferred = true
            decoder
        }) {
            if (!transferred) image.close()
        }
    }

    val data = Data.makeFromBytes(bytes)
    var codec: Codec? = null
    var image: Image? = null
    var transferred = false

    return cleaning({
        val parsed = Codec.makeFromData(data)
        codec = parsed

        val frameCount = parsed.frameCount
        val animated = request.animated && frameCount > 1

        if (animated) {
            require(frameCount <= 10000) { "Animation has too many frames" }
        }

        // Image.makeFromEncoded 应用 Exif orientation；Codec 的动画像素使用其画布尺寸
        val static = if (animated) null else Image.makeFromEncoded(bytes).also { image = it }
        val original = if (static != null) IntSize(static.width, static.height) else IntSize(parsed.width, parsed.height)
        val size = request.sampledSize(original)

        if (animated) {
            val working = if (parsed.encodedImageFormat == EncodedImageFormat.WEBP) size else original
            require(working.width.toLong() * working.height <= maxDecodePixels) { "Animation exceeds the frame pixel budget" }
        }
        val decoder = SkiaImageDecoder(data, parsed, static, original, size, animated, sampling = request.samplingMode)

        transferred = true
        decoder
    }) {
        if (!transferred) {
            image?.close()
            codec?.close()
            data.close()
        }
    }
}

private fun openStaticBitmapDecoder(bytes: ByteArray, request: ImageDecodeOptions): ImageDecoder? {
    // PNG 不支持 Codec 原生缩放；缩略图保留原有 Image 栅格路径，避免多余的工作位图和 Pixmap 包装
    if (PngImageFormat.matches(bytes) && bytes.size >= 24 && bytes.matchesImageSignature(12, "IHDR")) {
        val input = ByteArrayIO(bytes)
        val original = IntSize(input.readInt(16), input.readInt(20))
        if (request.sampledSize(original) != original) return null
    }

    return openStaticDataDecoder(Data.makeFromBytes(bytes), request)
}

/** 接管 data；文件入口可直接填充原生缓冲，PNG 缩放仍采用 Canvas 保持相同采样像素。 */
internal fun openStaticDataDecoder(data: Data, request: ImageDecodeOptions, fileInput: Boolean = false): ImageDecoder? =
    cleaning({
        val codec = Codec.makeFromData(data)

        cleaning(decode@{
            if (codec.colorSpace?.isSRGB == false) return@decode null
            if (fileInput && request.animated && codec.frameCount > 1) return@decode null

            val origin = codec.encodedOrigin
            if (origin == EncodedOrigin.UNUSED) return@decode null

            val raw = IntSize(codec.width, codec.height)
            val rotated = origin.swapsWidthHeight()
            val original = if (rotated) IntSize(raw.height, raw.width) else raw
            val size = request.sampledSize(original)
            val target = if (rotated) IntSize(size.height, size.width) else size

            val decodedSize = when (codec.encodedImageFormat) {
                EncodedImageFormat.WEBP -> target
                EncodedImageFormat.JPEG -> {
                    var sample = 1
                    while (sample < 8) {
                        val next = sample * 2
                        if ((raw.width + next - 1) / next < target.width || (raw.height + next - 1) / next < target.height) break
                        sample = next
                    }
                    IntSize((raw.width + sample - 1) / sample, (raw.height + sample - 1) / sample)
                }
                else -> raw
            }

            val bitmap = Bitmap()
            var bitmapTransferred = false

            cleaning({
                check(bitmap.allocPixels(ImageInfo.makeN32Premul(decodedSize.width, decodedSize.height, ColorSpace.sRGB)))

                val error = catchingError { codec.readPixels(bitmap) }
                if (error != null) {
                    if (error is IllegalArgumentException || error is UnsupportedOperationException) return@decode null
                    throw error
                }
                bitmap.setImmutable()

                val canvasScaling = fileInput && codec.encodedImageFormat == EncodedImageFormat.PNG && decodedSize != size
                if (origin == EncodedOrigin.TOP_LEFT && !canvasScaling) {
                    // 同尺寸 PNG / BMP 直接移交像素，WebP 由 Codec 在目标尺寸解码
                    return@decode SkiaBitmapDecoder(bitmap, original, size, request.samplingMode).also { bitmapTransferred = true }
                }

                val image = Image.makeFromBitmap(bitmap)
                var transferred = false

                cleaning({
                    SkiaImageDecoder(null, null, image, original, size, false, origin, request.samplingMode).also { transferred = true }
                }) {
                    if (!transferred) image.close()
                }
            }) {
                if (!bitmapTransferred) bitmap.close()
            }
        }, codec::close)
    }, data::close)
