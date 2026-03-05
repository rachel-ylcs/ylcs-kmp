package love.yinlin.uri

import android.content.Context
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

open class ContentUri(
    private val context: Context,
    override val path: String
) : ImplicitUri {
    override suspend fun <R> read(block: suspend (Source) -> R): R = context.contentResolver.openInputStream(Uri.parse(path)!!.toAndroidUri())!!.asSource().buffered().use { block(it) }
    override suspend fun write(block: suspend (Sink) -> Unit) = context.contentResolver.openOutputStream(Uri.parse(path)!!.toAndroidUri())!!.asSink().buffered().use { block(it) }
}