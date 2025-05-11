package love.yinlin.platform

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.buffered
import platform.Foundation.NSInputStream
import platform.Foundation.NSURL

class SandboxSource(val url: NSURL) : RawSource {
    val source: RawSource = NSInputStream(uRL = url).asSource()

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long = source.readAtMostTo(sink, byteCount)

    override fun close() {
        source.close()
        url.stopAccessingSecurityScopedResource()
    }
}

open class SandboxPath(val url: NSURL) : ImplicitPath {
    override val path: String get() = url.path!!
    override val source: Source get() = SandboxSource(url).buffered()
}