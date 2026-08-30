package love.yinlin.io

import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.readByteArray
import love.yinlin.fs.FileSystemWritableFileStream
import org.khronos.webgl.toInt8Array
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
class WebFileSink(private val stream: FileSystemWritableFileStream) : RawSink {
    override fun write(source: Buffer, byteCount: Long) {
        stream.write(source.readByteArray(byteCount.toInt()).toInt8Array())
    }

    override fun flush() { }

    override fun close() { stream.close() }
}