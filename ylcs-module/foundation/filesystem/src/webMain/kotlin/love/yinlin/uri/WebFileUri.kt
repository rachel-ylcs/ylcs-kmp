@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.uri

import kotlinx.coroutines.await
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source
import love.yinlin.fs.FileSystemFileHandle
import love.yinlin.fs.openSource
import org.w3c.files.File
import kotlin.uuid.Uuid

/** 浏览器通过选取、拖放、文件关联或分享交给应用的只读文件。 */
class WebFileUri private constructor(override val path: String, private val getFile: suspend () -> File) : ImplicitUri {
    constructor(file: File) : this(file.name, { file })
    constructor(handle: FileSystemFileHandle) : this(handle.name, { handle.getFile().await() })

    override suspend fun <R> read(block: suspend (Source) -> R): R = getFile().openSource().use { block(it) }

    override suspend fun write(block: suspend (Sink) -> Unit): Unit = throw IOException("The browser grants read access to this file")

    companion object {
        private const val LAUNCH_AUTHORITY = "web-file"
        private var pendingLaunch: Pair<Uri, WebFileUri>? = null

        fun registerLaunch(handle: FileSystemFileHandle): Uri = registerLaunch(WebFileUri(handle))

        fun registerLaunch(file: File): Uri = registerLaunch(WebFileUri(file))

        /** 为浏览器交付的文件生成可传入导航的临时标识。 */
        private fun registerLaunch(file: WebFileUri): Uri {
            val uri = Uri(Scheme.Content, host = LAUNCH_AUTHORITY, path = "/${Uuid.generateV7()}")
            pendingLaunch = uri to file
            return uri
        }

        fun resolve(uri: Uri): ImplicitUri {
            if (uri.scheme != Scheme.Content || uri.host != LAUNCH_AUTHORITY) return RegularUri(uri.toString())
            val launch = pendingLaunch
            check(launch != null && launch.first == uri) { "请重新打开该文件" }
            pendingLaunch = null
            return launch.second
        }
    }
}
