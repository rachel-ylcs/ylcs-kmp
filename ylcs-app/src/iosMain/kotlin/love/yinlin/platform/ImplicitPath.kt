package love.yinlin.platform

import kotlinx.io.*
import platform.Foundation.NSInputStream
import platform.Foundation.NSOutputStream
import platform.Foundation.NSURL

class SandboxSource(val url: NSURL) : RawSource {
    val source: RawSource

    init {
        val canAccess = url.startAccessingSecurityScopedResource()
        if (!canAccess) {
            throw IOException()
        }
        source = NSInputStream(uRL = url).asSource()
    }

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long = source.readAtMostTo(sink, byteCount)

    override fun close() {
        source.close()
        url.stopAccessingSecurityScopedResource()
    }
}

class SandboxSink(val url: NSURL) : RawSink {
    val sink: RawSink
    init {
        val canAccess = url.startAccessingSecurityScopedResource()
        if (!canAccess) {
            throw IOException()
        }
        sink = NSOutputStream(uRL = url, append = false).asSink()
    }

    override fun write(source: Buffer, byteCount: Long) {
        sink.write(source, byteCount)
    }

    override fun flush() {
        sink.flush()
    }

    override fun close() {
        sink.close()
        url.stopAccessingSecurityScopedResource()
    }
}

open class SandboxPath(val url: NSURL) : ImplicitPath {
    override val path: String get() = url.path!!
    override val source: Source get() = SandboxSource(url).buffered()
    override val sink: Sink get() = SandboxSink(url).buffered()
}