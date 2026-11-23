package love.yinlin.foundation.picker

import kotlinx.coroutines.await
import kotlinx.io.Sink
import kotlinx.io.Source
import love.yinlin.fs.FileSystemFileHandle
import love.yinlin.fs.openSource
import love.yinlin.fs.write
import love.yinlin.uri.ImplicitUri

@OptIn(ExperimentalWasmJsInterop::class)
internal class SaveFileUri(private val handle: FileSystemFileHandle) : ImplicitUri {
    override val path: String get() = handle.name
    override suspend fun <R> read(block: suspend (Source) -> R): R = handle.getFile().await().openSource().use { block(it) }
    override suspend fun write(block: suspend (Sink) -> Unit) = handle.write(block)
}
