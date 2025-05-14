package love.yinlin.platform

import kotlinx.io.*
import platform.Foundation.NSInputStream
import platform.Foundation.NSOutputStream
import platform.Foundation.NSURL

class SandboxSource(val url: NSURL) : RawSource {
    val source: RawSource

    init {
        val canAccess = url.startAccessingSecurityScopedResource()
        // 应用沙箱内部的文件调用该接口会返回false
        // if (!canAccess) {
        //     throw IOException()
        // }
        source = NSInputStream(uRL = url).asSource()
    }

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long = source.readAtMostTo(sink, byteCount)

    override fun close() {
        source.close()
        url.stopAccessingSecurityScopedResource()
    }
}

class SandboxSink(val url: NSURL, val onClosed: () -> Unit = {}) : RawSink {
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

open class SandboxPath(val url: NSURL, val parentUrl: NSURL? = null) : ImplicitPath {
    init {
        parentUrl?.let {
            val canAccess = parentUrl.startAccessingSecurityScopedResource()
            if (!canAccess) {
                throw IOException()
            }
        }
    }

    override val path: String get() = url.path!!
    override val source: Source get() = SandboxSource(url).buffered()
    override val sink: Sink get() = SandboxSink(url) {
        parentUrl?.stopAccessingSecurityScopedResource()
    }.buffered()
}