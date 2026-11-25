package love.yinlin.compose.image.decoder

import android.graphics.ColorSpace
import android.graphics.ImageDecoder as AndroidDecoder
import androidx.compose.ui.unit.IntSize
import love.yinlin.compose.image.ImageManager
import love.yinlin.compose.image.data.GifInfo
import love.yinlin.compose.image.data.ImageDecodeOptions
import love.yinlin.compose.image.data.WebpInfo
import java.nio.ByteBuffer
import love.yinlin.compose.image.data.PlatformImageAnimation
import love.yinlin.extension.catchingNull

internal actual fun ImageDecoder.platformAnimation(): PlatformImageAnimation? = if (this is AndroidSystemWebpDecoder) takePlatformAnimation() else null

internal actual fun openPlatformImageDecoder(bytes: ByteArray, request: ImageDecodeOptions): ImageDecoder {
    if (!request.animated) return decodeBitmap(bytes, request)

    val maxDecodePixels = ImageManager.configuration.maxDecodePixels

    val gif = bytes.size >= 6 && bytes.decodeToString(0, 3) == "GIF"
    val webp = bytes.size >= 30 && bytes.decodeToString(0, 4) == "RIFF" && bytes.decodeToString(8, 16) == "WEBPVP8X" && bytes[20].toInt() and 2 != 0

    return when {
        gif -> {
            val info = GifInfo(bytes)
            require(info.size.width.toLong() * info.size.height <= maxDecodePixels) { "Animation exceeds the frame pixel budget" }
            AndroidGifDecoder(bytes, info, request)
        }
        webp -> {
            val info = WebpInfo(bytes)
            require(info.size.width.toLong() * info.size.height <= maxDecodePixels) { "Animation exceeds the frame pixel budget" }

            val native = if (info.frames.size > 1 && request.platformAnimationAllowed) {
                catchingNull { AndroidSystemWebpDecoder(bytes, info, request) }
            } else null

            native ?: AndroidWebpDecoder(bytes, info, request)
        }
        else -> decodeBitmap(bytes, request)
    }
}

private fun decodeBitmap(bytes: ByteArray, request: ImageDecodeOptions): ImageDecoder {
    var original = IntSize.Zero
    val source = AndroidDecoder.createSource(ByteBuffer.wrap(bytes))
    val bitmap = AndroidDecoder.decodeBitmap(source) { decoder, info, _ ->
        original = IntSize(info.size.width, info.size.height)
        val size = request.sampledSize(original)
        decoder.allocator = if (request.hardwareAllowed) AndroidDecoder.ALLOCATOR_HARDWARE else AndroidDecoder.ALLOCATOR_SOFTWARE
        decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
        decoder.setTargetSize(size.width, size.height)
    }
    return AndroidBitmapDecoder(bitmap, original)
}