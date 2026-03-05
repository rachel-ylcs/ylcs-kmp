package love.yinlin.uri

import kotlinx.io.Sink
import kotlinx.io.Source

interface ImplicitUri {
    val path: String
    suspend fun <R> read(block: suspend (Source) -> R): R
    suspend fun write(block: suspend (Sink) -> Unit)
}