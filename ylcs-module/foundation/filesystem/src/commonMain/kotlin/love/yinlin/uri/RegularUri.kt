package love.yinlin.uri

import kotlinx.io.Sink
import kotlinx.io.Source
import love.yinlin.fs.File

open class RegularUri(override val path: String) : ImplicitUri {
    override suspend fun <R> read(block: suspend (Source) -> R): R = File(path).read(block)
    override suspend fun write(block: suspend (Sink) -> Unit) = File(path).write(block)
}