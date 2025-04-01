package love.yinlin.common

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray

class ArrayBufferSource(buffer: ArrayBuffer): RawSource {
    private var position = 0L
    private val bytes = Int8Array(buffer).toByteArray()

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (byteCount == 0L) return 0L
        require(byteCount >= 0L) { "byteCount $byteCount < 0" }
        val endPos = minOf(position + byteCount, bytes.size.toLong())
        val readTotal = endPos - position
        if (readTotal == 0L) return -1L
        sink.write(bytes, position.toInt(), endPos.toInt())
        position = endPos
        return readTotal
    }

    override fun close() {}

    override fun toString(): String = "ArrayBufferSource"
}