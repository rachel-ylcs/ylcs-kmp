package love.yinlin.common.uri

import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import platform.Foundation.NSURL

open class SandboxUri(val url: NSURL, val parentUrl: NSURL? = null) : ImplicitUri {
    init {
        parentUrl?.let {
            val canAccess = parentUrl.startAccessingSecurityScopedResource()
            if (!canAccess) throw IOException()
        }
    }

    override val path: String get() = url.path!!
    override val source: Source get() = SandboxSource(url).buffered()
    override val sink: Sink get() = SandboxSink(url) { parentUrl?.stopAccessingSecurityScopedResource() }.buffered()
}