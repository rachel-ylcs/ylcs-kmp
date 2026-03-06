package love.yinlin.io

import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.readByteArray
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.ByteArrayCompatible
import love.yinlin.fs.FileSystemWritableFileStream
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class, CompatibleRachelApi::class)
class WebFileSink(private val stream: FileSystemWritableFileStream) : RawSink {
    override fun write(source: Buffer, byteCount: Long) {
        stream.write(ByteArrayCompatible(source.readByteArray(byteCount.toInt())).asInt8Array)
    }

    override fun flush() { }

    override fun close() { stream.close() }
}