package love.yinlin.io

import kotlinx.coroutines.await
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.readByteArray
import love.yinlin.fs.FileSystemWritableFileStream
import org.khronos.webgl.toInt8Array
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
class WebFileSink(private val stream: FileSystemWritableFileStream) : RawSink {
    private var closing: Promise<JsAny?>? = null

    override fun write(source: Buffer, byteCount: Long) {
        stream.write(source.readByteArray(byteCount.toInt()).toInt8Array())
    }

    override fun flush() { }

    override fun close() {
        if (closing == null) closing = stream.close()
    }

    internal suspend fun awaitClosed() { closing?.await() }
}