package love.yinlin.compose.image.decoder

import androidx.compose.ui.unit.IntSize
import love.yinlin.compose.image.ImageManager
import love.yinlin.compose.image.data.ImageDecodeOptions
import love.yinlin.compose.image.data.ImageRequest
import kotlin.math.min
import kotlin.math.sqrt

internal fun openImageDecoder(bytes: ByteArray, request: ImageRequest): ImageDecoder = ImageManager.loader.components.open(bytes, request.decodeOptions())

internal expect fun openPlatformImageDecoder(bytes: ByteArray, request: ImageDecodeOptions): ImageDecoder

internal fun ByteArray.matchesImageSignature(offset: Int, signature: String): Boolean {
    if (size < offset + signature.length) return false
    for (index in signature.indices) {
        if (this[offset + index] != signature[index].code.toByte()) return false
    }
    return true
}

internal fun sampledImageSize(original: IntSize, request: ImageDecodeOptions): IntSize {
    val maxDecodePixels = ImageManager.configuration.maxDecodePixels
    require(original.width > 0 && original.height > 0) { "Invalid image dimensions" }
    require(original.width.toLong() * original.height <= 64L * 1024 * 1024) { "Image exceeds the source pixel budget" }

    var multiplier = 1.0
    request.targetSize?.let { target ->
        val sx = target.width.toDouble() * request.sizeMultiplier / original.width
        val sy = target.height.toDouble() * request.sizeMultiplier / original.height
        multiplier = min(1.0, if (request.scaleToFill) maxOf(sx, sy) else minOf(sx, sy))
    }
    multiplier = min(multiplier, sqrt(maxDecodePixels.toDouble() / (original.width.toDouble() * original.height)))
    multiplier = min(multiplier, min(16384.0 / original.width, 16384.0 / original.height))

    val height = minOf(maxOf(1, (original.height * multiplier).toInt()), maxDecodePixels.toInt())
    val width = minOf(maxOf(1, (original.width * multiplier).toInt()), (maxDecodePixels / height).toInt())
    return IntSize(width, height)
}

internal fun normalizedFrameDuration(milliseconds: Int): Int = if (milliseconds < 20) 100 else milliseconds
