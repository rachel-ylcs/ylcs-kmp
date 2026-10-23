package love.yinlin.compose.image.input

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import love.yinlin.fs.File
import java.io.ByteArrayOutputStream
import java.io.FileInputStream

internal actual suspend fun readPlatformImageFile(file: File, maxBytes: Int): ByteArray? {
    // 私有 JvmFile 的相等比较只接受同类，避免绕过调用方提供的 File 实现
    if (File(file.path) != file) return null

    val context = currentCoroutineContext()
    context.ensureActive()

    require(java.io.File(file.path).isFile) { "Image input is not a regular file" }

    @Suppress("BlockingMethodInNonBlockingContext")
    return FileInputStream(file.path).use { input ->
        val size = input.channel.size()
        require(size in 1L .. maxBytes.toLong()) { "Image file is empty or exceeds the byte budget" }

        val bytes = ByteArray(size.toInt())
        var offset = 0

        while (offset < bytes.size) {
            context.ensureActive()
            val count = input.read(bytes, offset, minOf(bytes.size - offset, 65536))
            if (count < 0) break
            offset += count
        }

        context.ensureActive()

        if (offset < bytes.size) {
            require(offset > 0) { "Empty image file" }
            return@use bytes.copyOf(offset)
        }

        val first = input.read()
        if (first < 0) return@use bytes
        require(bytes.size < maxBytes) { "Image file exceeds the byte budget" }

        // 只有文件在读取期间增长才创建扩展缓冲，稳定长度文件直接移交最终数组
        val output = ByteArrayOutputStream(bytes.size)
        output.write(bytes)
        output.write(first)
        val chunk = ByteArray(8192)

        while (true) {
            context.ensureActive()
            val count = input.read(chunk)
            if (count < 0) break
            require(output.size().toLong() + count <= maxBytes) { "Image file exceeds the byte budget" }
            output.write(chunk, 0, count)
        }

        output.toByteArray()
    }
}