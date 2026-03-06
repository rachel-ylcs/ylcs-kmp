package love.yinlin.io

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.ByteArrayCompatible
import love.yinlin.extension.asByteArray
import org.w3c.files.File
import org.w3c.files.FileReaderSync

@OptIn(CompatibleRachelApi::class)
class WebFileSource(private val file: File) : RawSource {
    private var position = 0
    private val bytes by lazy { ByteArrayCompatible(FileReaderSync().readAsArrayBuffer(file).asByteArray) }

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

    override fun close() {
        if (!file.isClosed) file.close()
    }

    override fun toString(): String = "WebFileSource"
}