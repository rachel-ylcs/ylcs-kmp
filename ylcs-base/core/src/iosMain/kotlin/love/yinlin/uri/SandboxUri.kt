package love.yinlin.uri

import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import love.yinlin.io.SandboxSink
import love.yinlin.io.SandboxSource
import platform.Foundation.NSURL

open class SandboxUri(
    private val url: NSURL,
    private val parentUrl: NSURL? = null
) : ImplicitUri {
    init {
        parentUrl?.let {
            val canAccess = parentUrl.startAccessingSecurityScopedResource()
            if (!canAccess) throw IOException()
        }
    }

    override val path: String get() = url.path!!

    override suspend fun <R> read(block: suspend (Source) -> R): R = SandboxSource(url).buffered().use { block(it) }

    override suspend fun write(block: suspend (Sink) -> Unit) = SandboxSink(url) { parentUrl?.stopAccessingSecurityScopedResource() }.buffered().use { block(it) }
}