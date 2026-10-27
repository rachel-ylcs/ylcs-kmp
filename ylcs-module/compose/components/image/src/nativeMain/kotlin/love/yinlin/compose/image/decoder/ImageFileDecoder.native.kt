@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.image.decoder

import kotlinx.cinterop.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import love.yinlin.compose.image.data.ImageDecodeOptions
import love.yinlin.compose.image.data.ImageOrigin
import love.yinlin.compose.image.data.ImageResult
import love.yinlin.compose.image.input.NativeImageFile
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.cleaning
import love.yinlin.extension.throwing
import love.yinlin.fs.File
import org.jetbrains.skia.Data

internal actual val supportsPlatformImageFiles: Boolean = true

internal actual suspend fun decodePlatformImageFile(file: File, request: ImageDecodeOptions, maxBytes: Int): ImageResult? {
    var encoded: Data? = null

    return cleaning({
        val supported = Coroutines.io {
            val context = currentCoroutineContext()
            context.ensureActive()

            NativeImageFile.open(file, maxBytes)?.use { input ->
                val header = ByteArray(minOf(input.size, 32))
                if (input.read(header) != header.size) return@use false
                if (!PngImageFormat.matches(header) && !WebpImageFormat.matches(header)) return@use false
                if (request.animated && WebpImageFormat.matches(header) && header.size >= 30 && header.matchesImageSignature(12, "VP8X") && header[20].toInt() and 2 != 0) return@use false
                if (!input.rewind()) return@use false

                val data = Data.makeUninitialized(input.size)
                encoded = data
                val pointer = checkNotNull(interpretCPointer<ByteVar>(data.writableData()))
                var offset = 0

                while (offset < input.size) {
                    context.ensureActive()
                    val count = input.read(checkNotNull(pointer + offset), minOf(input.size - offset, 65536))
                    if (count == 0) return@use false
                    offset += count
                }

                context.ensureActive()
                input.hasSameSize()
            } ?: false
        }

        if (!supported) return@cleaning null

        throwing(::ImageDecodeException) {
            Coroutines.cpu {
                val data = checkNotNull(encoded)
                encoded = null // 从这里起由 openStaticDataDecoder 接管并释放

                val decoder = openStaticDataDecoder(data, request, fileInput = true) ?: return@cpu null

                cleaning({
                    val frame = decoder.frame(0)
                    require(frame.width == decoder.size.width && frame.height == decoder.size.height)
                    ImageResult(frame, decoder.originalSize, ImageOrigin.Local)
                }, decoder::close)
            }
        }
    }) { encoded?.close() }
}
