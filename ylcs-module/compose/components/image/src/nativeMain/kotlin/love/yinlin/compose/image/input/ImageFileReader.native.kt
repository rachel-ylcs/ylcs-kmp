package love.yinlin.compose.image.input

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import love.yinlin.fs.File

internal actual suspend fun readPlatformImageFile(file: File, maxBytes: Int): ByteArray? {
    val context = currentCoroutineContext()
    context.ensureActive()

    return NativeImageFile.open(file, maxBytes)?.use { input ->
        val bytes = ByteArray(input.size)
        var offset = 0

        while (offset < bytes.size) {
            context.ensureActive()
            val count = input.read(bytes, offset, minOf(bytes.size - offset, 65536))
            if (count == 0) break
            offset += count
        }

        context.ensureActive()
        if (offset < bytes.size) {
            require(offset > 0) { "Empty image file" }
            return@use bytes.copyOf(offset)
        }

        val chunk = ByteArray(8192)
        var count = input.read(chunk)
        if (count == 0) return@use bytes

        val output = Buffer()
        output.write(bytes)

        while (count > 0) {
            context.ensureActive()
            require(output.size + count <= maxBytes.toLong()) { "Image file exceeds the byte budget" }
            output.write(chunk, 0, count)
            count = input.read(chunk)
        }

        context.ensureActive()
        output.readByteArray()
    }
}
