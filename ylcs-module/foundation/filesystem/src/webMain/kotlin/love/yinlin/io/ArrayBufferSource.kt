package love.yinlin.io

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray

class ArrayBufferSource(buffer: ArrayBuffer) : RawSource {
    private var buffer: ArrayBuffer? = buffer
    private var position = 0

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        val data = checkNotNull(buffer) { "Source is closed" }
        if (byteCount == 0L) return 0L
        require(byteCount >= 0L) { "byteCount $byteCount < 0" }
        val count = minOf(byteCount, (data.byteLength - position).toLong(), 64L * 1024).toInt()
        if (count == 0) return -1L
        sink.write(Int8Array(data, position, count).toByteArray())
        position += count
        return count.toLong()
    }

    override fun close() {
        buffer = null
    }

    override fun toString(): String = "ArrayBufferSource"
}
