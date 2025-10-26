package love.yinlin.common.uri

import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.asSink
import platform.Foundation.NSOutputStream
import platform.Foundation.NSURL

class SandboxSink(url: NSURL, val onClosed: () -> Unit = {}) : RawSink {
    val sink = NSOutputStream(uRL = url, append = false).asSink()

    override fun write(source: Buffer, byteCount: Long) {
        sink.write(source, byteCount)
    }

    override fun flush() {
        sink.flush()
    }

    override fun close() {
        sink.close()
        onClosed()
    }
}