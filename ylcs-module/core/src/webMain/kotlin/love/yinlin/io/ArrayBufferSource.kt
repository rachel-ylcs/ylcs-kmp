package love.yinlin.io

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.ByteArrayCompatible
import love.yinlin.extension.asByteArray
import org.khronos.webgl.ArrayBuffer

@OptIn(CompatibleRachelApi::class)
class ArrayBufferSource(buffer: ArrayBuffer) : RawSource {
    private var position = 0
    private val bytes by lazy { ByteArrayCompatible(buffer.asByteArray) }

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (byteCount == 0L) return 0L
        require(byteCount >= 0L) { "byteCount $byteCount < 0" }
        val endPos = minOf((position + byteCount).toInt(), bytes.size)
        val readTotal = endPos - position
        if (readTotal == 0) return -1L
        sink.write(bytes.raw, position, endPos)
        position = endPos
        return readTotal.toLong()
    }

    override fun close() {}

    override fun toString(): String = "ArrayBufferSource"
}