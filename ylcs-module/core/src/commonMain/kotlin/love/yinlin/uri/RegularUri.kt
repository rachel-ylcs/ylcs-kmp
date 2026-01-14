package love.yinlin.uri

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.files.Path
import love.yinlin.extension.read
import love.yinlin.extension.write

open class RegularUri(
    override val path: String
) : ImplicitUri {
    override suspend fun <R> read(block: suspend (Source) -> R): R = Path(path).read(block)
    override suspend fun write(block: suspend (Sink) -> Unit) = Path(path).write(block)
}