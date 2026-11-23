@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compose.image.decoder

import kotlinx.coroutines.ensureActive
import love.yinlin.compose.image.data.ImageDecodeOptions
import love.yinlin.compose.image.data.ImageOrigin
import love.yinlin.compose.image.data.ImageResult
import love.yinlin.compose.image.input.imageSnapshot
import love.yinlin.compose.image.input.readImageBytes
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.cleaning
import love.yinlin.extension.throwing
import love.yinlin.fs.File
import org.jetbrains.skia.Data

internal actual val supportsPlatformImageFiles: Boolean = true

internal actual suspend fun decodePlatformImageFile(file: File, request: ImageDecodeOptions, maxBytes: Int): ImageResult? {
    val snapshot = file.imageSnapshot(maxBytes) ?: return null
    val header = snapshot.slice(0, minOf(snapshot.size.toDouble().toInt(), 32)).readImageBytes()
    val webp = WebpImageFormat.matches(header)
    if (!webp && !PngImageFormat.matches(header)) return null
    if (request.animated && webp && header.size >= 30 && header.matchesImageSignature(12, "VP8X") && header[20].toInt() and 2 != 0) return null

    val bytes = snapshot.readImageBytes()
    return throwing(::ImageDecodeException) {
        Coroutines.cpu {
            val decoder = openStaticDataDecoder(Data.makeFromBytes(bytes), request, fileInput = true) ?: return@cpu null
            cleaning({
                val image = decoder.frame(0)
                require(image.width == decoder.size.width && image.height == decoder.size.height)
                ensureActive()
                ImageResult(image, decoder.originalSize, ImageOrigin.Local)
            }, decoder::close)
        }
    }
}
