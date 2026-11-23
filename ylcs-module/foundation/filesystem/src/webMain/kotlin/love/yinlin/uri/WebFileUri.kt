@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.uri

import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source
import love.yinlin.fs.openSource
import org.w3c.files.File

/** 浏览器通过选取或拖放交给应用的只读文件。 */
class WebFileUri(private val file: File) : ImplicitUri {
    override val path: String get() = file.name

    override suspend fun <R> read(block: suspend (Source) -> R): R = file.openSource().use { block(it) }

    override suspend fun write(block: suspend (Sink) -> Unit): Unit = throw IOException("The browser grants read access to this file")
}
