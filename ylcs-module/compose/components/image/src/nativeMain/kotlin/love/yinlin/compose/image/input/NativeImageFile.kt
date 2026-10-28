@file:OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
package love.yinlin.compose.image.input

import kotlinx.cinterop.*
import kotlinx.io.IOException
import love.yinlin.extension.cleaning
import love.yinlin.fs.File
import platform.posix.*

internal class NativeImageFile private constructor(private val stream: CPointer<FILE>, val size: Int) : AutoCloseable {
    companion object {
        fun open(file: File, maxBytes: Int): NativeImageFile? {
            if (File(file.path) != file) return null
            require('\u0000' !in file.path) { "Image file path contains a null character" }

            val stream = openNativeImageFile(file.path)
            var transferred = false

            return cleaning({
                if (fseek(stream, 0, SEEK_END) != 0) throw IOException("Cannot seek image file")
                val length = ftell(stream)
                require(length in 1 .. maxBytes) { "Image file is empty or exceeds the byte budget" }
                if (fseek(stream, 0, SEEK_SET) != 0) throw IOException("Cannot rewind image file")

                NativeImageFile(stream, length).also { transferred = true }
            }) { if (!transferred) fclose(stream) }
        }
    }

    fun read(destination: CPointer<ByteVar>, count: Int): Int {
        val read = fread(destination, 1.convert(), count.convert(), stream).toInt()
        if (ferror(stream) != 0) throw IOException("Cannot read image file")
        return read
    }

    fun read(destination: ByteArray, offset: Int = 0, count: Int = destination.size - offset): Int = destination.usePinned { read(it.addressOf(offset), count) }

    fun rewind(): Boolean = fseek(stream, 0, SEEK_SET) == 0

    fun hasSameSize(): Boolean = fseek(stream, 0, SEEK_END) == 0 && ftell(stream) == size

    override fun close() {
        fclose(stream)
    }
}

internal expect fun openNativeImageFile(path: String): CPointer<FILE>
