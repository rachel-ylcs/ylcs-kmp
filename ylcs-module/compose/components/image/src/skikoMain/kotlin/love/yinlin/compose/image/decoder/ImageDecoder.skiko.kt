package love.yinlin.compose.image.decoder

import androidx.compose.ui.unit.IntSize
import love.yinlin.compose.image.ImageManager
import love.yinlin.compose.image.data.ImageDecodeOptions
import love.yinlin.extension.cleaning
import org.jetbrains.skia.*

internal actual fun openPlatformImageDecoder(bytes: ByteArray, request: ImageDecodeOptions): ImageDecoder {
    val maxDecodePixels = ImageManager.configuration.maxDecodePixels
    // 已知首帧模式无需创建 Codec 或读取帧数/帧表。静态格式默认也走此路径。
    if (!request.animated) {
        val image = Image.makeFromEncoded(bytes)
        var transferred = false
        return cleaning({
            val original = IntSize(image.width, image.height)
            val size = request.sampledSize(original)
            val decoder = SkiaImageDecoder(null, null, image, original, size, false)
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
        val animated = request.animated && parsed.frameCount > 1
        if (animated) {
            require(parsed.frameCount <= 10000) { "Animation has too many frames" }
            require(parsed.width.toLong() * parsed.height <= maxDecodePixels) { "Animation exceeds the frame pixel budget" }
        }

        // Image.makeFromEncoded 应用 Exif orientation；Codec 的动画像素使用其画布尺寸
        val static = if (animated) null else Image.makeFromEncoded(bytes).also { image = it }
        val original = if (static != null) IntSize(static.width, static.height) else IntSize(parsed.width, parsed.height)
        val size = request.sampledSize(original)
        val decoder = SkiaImageDecoder(data, parsed, static, original, size, animated)
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
