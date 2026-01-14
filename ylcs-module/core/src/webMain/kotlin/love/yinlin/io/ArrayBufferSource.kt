package love.yinlin.io

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray

class ArrayBufferSource(buffer: ArrayBuffer) : RawSource {
    private var position = 0
    private val bufferArray = Int8Array(buffer)

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (byteCount == 0L) return 0L
        require(byteCount >= 0L) { "byteCount $byteCount < 0" }
        val endPos = minOf((position + byteCount).toInt(), bufferArray.length)
        val readTotal = endPos - position
        if (readTotal == 0) return -1L
        val data = bufferArray.subarray(position, endPos).toByteArray()
        sink.write(data, 0, readTotal)
        position = endPos
        return readTotal.toLong()
    }

    override fun close() {}

    override fun toString(): String = "ArrayBufferSource"
}