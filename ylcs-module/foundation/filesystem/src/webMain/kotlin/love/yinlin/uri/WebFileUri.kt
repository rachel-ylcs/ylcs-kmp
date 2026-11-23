@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.uri

import kotlinx.coroutines.await
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source
import love.yinlin.fs.FileSystemFileHandle
import love.yinlin.fs.openSource
import org.w3c.files.File

/** 浏览器通过选取、拖放或系统文件关联交给应用的只读文件。 */
class WebFileUri private constructor(override val path: String, private val getFile: suspend () -> File) : ImplicitUri {
    constructor(file: File) : this(file.name, { file })
    constructor(handle: FileSystemFileHandle) : this(handle.name, { handle.getFile().await() })

    override suspend fun <R> read(block: suspend (Source) -> R): R = getFile().openSource().use { block(it) }

    override suspend fun write(block: suspend (Sink) -> Unit): Unit = throw IOException("The browser grants read access to this file")
}
