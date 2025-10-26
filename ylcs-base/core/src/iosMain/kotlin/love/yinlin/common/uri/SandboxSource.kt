package love.yinlin.common.uri

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.asSource
import platform.Foundation.NSInputStream
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