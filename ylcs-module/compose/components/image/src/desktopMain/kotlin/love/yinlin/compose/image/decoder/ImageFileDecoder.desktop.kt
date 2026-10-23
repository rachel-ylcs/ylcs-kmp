package love.yinlin.compose.image.decoder

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import love.yinlin.compose.image.data.ImageDecodeOptions
import love.yinlin.compose.image.data.ImageOrigin
import love.yinlin.compose.image.data.ImageResult
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.cleaning
import love.yinlin.extension.catchingNull
import love.yinlin.extension.throwing
import love.yinlin.fs.File
import org.jetbrains.skia.Data
import java.io.FileInputStream
import java.lang.foreign.MemorySegment

internal actual val supportsPlatformImageFiles: Boolean = true

internal actual suspend fun decodePlatformImageFile(file: File, request: ImageDecodeOptions, maxBytes: Int): ImageResult? {
    // 私有 JvmFile 的相等比较只接受同类，自定义文件实现保留其自己的读取语义
    if (File(file.path) != file) return null

    var encoded: Data? = null

    return cleaning({
        val supported = Coroutines.io {
            val context = currentCoroutineContext()
            context.ensureActive()

            require(java.io.File(file.path).isFile) { "Image input is not a regular file" }

            @Suppress("BlockingMethodInNonBlockingContext")
            FileInputStream(file.path).use { input ->
                val channel = input.channel
                val length = channel.size()
                require(length in 1L .. maxBytes.toLong()) { "Image file is empty or exceeds the byte budget" }

                val header = ByteArray(minOf(length.toInt(), 32))
                var read = 0

                while (read < header.size) {
                    val count = input.read(header, read, header.size - read)
                    if (count < 0) return@use false
                    read += count
                }

                if (!PngImageFormat.matches(header) && !WebpImageFormat.matches(header)) return@use false
                if (request.animated && WebpImageFormat.matches(header) && header.size >= 30 && header.matchesImageSignature(12, "VP8X") && header[20].toInt() and 2 != 0) return@use false

                val data = Data.makeUninitialized(length.toInt())
                encoded = data

                val buffer = catchingNull { MemorySegment.ofAddress(data.writableData()).reinterpret(length).asByteBuffer() } ?: return@use false
                channel.position(0)

                while (buffer.hasRemaining()) {
                    context.ensureActive()
                    buffer.limit(buffer.position() + minOf(buffer.remaining(), 65536))
                    if (channel.read(buffer) < 0) return@use false
                    buffer.limit(buffer.capacity())
                }

                context.ensureActive()

                // 文件截断或增长时交回通用读取器重新获取，避免发布部分写入的数据
                channel.size() == length
            }
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