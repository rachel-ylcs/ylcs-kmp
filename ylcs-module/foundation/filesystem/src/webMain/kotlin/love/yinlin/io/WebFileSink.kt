package love.yinlin.io

import kotlinx.coroutines.await
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.readByteArray
import love.yinlin.extension.catchingError
import love.yinlin.fs.FileSystemWritableFileStream
import org.khronos.webgl.toInt8Array
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise
import kotlin.js.js

// RawSink 调用者可能稍后才等待结果。保留原 Promise 的拒绝原因，同时避免未处理的拒绝。
@OptIn(ExperimentalWasmJsInterop::class)
private fun observe(promise: Promise<JsAny?>): Promise<JsAny?> = js("""
    (function(pending) {
        pending.catch(() => {});
        return pending;
    })(promise)
""")

@OptIn(ExperimentalWasmJsInterop::class)
class WebFileSink(private val stream: FileSystemWritableFileStream) : RawSink {
    private var lastWrite: Promise<JsAny?>? = null
    private var closing: Promise<JsAny?>? = null
    private var closed = false
    private var deferred = false

    override fun write(source: Buffer, byteCount: Long) {
        require(byteCount in 0L .. source.size)

        check(!closed) { "Sink is closed" }

        var remaining = byteCount

        while (remaining > 0) {
            val count = minOf(remaining, 64L * 1024)
            lastWrite = observe(stream.write(source.readByteArray(count.toInt()).toInt8Array()))
            remaining -= count
        }
    }

    override fun flush() { }

    override fun close() {
        closed = true
        if (!deferred) commit()
    }

    internal fun deferCommit() {
        deferred = true
    }

    internal fun commit() {
        if (closing == null) closing = observe(stream.close())
    }

    internal fun abort() {
        if (closing != null) return

        closed = true
        lastWrite = null
        closing = observe(stream.abort())
    }

    // 在 [close] 之后等待 OPFS 提交并传播写入失败
    suspend fun awaitClosed() {
        val completion = checkNotNull(closing) { "Close the sink before awaiting its completion" }
        val failure = catchingError { completion.await() } ?: return
        lastWrite?.await()
        throw failure
    }
}
