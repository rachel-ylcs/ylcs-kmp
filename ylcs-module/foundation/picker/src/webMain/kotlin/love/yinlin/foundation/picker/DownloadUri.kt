@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.foundation.picker

import kotlinx.browser.window
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readByteArray
import love.yinlin.extension.cleaning
import love.yinlin.extension.createElement
import love.yinlin.uri.ImplicitUri
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toInt8Array
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob

private fun downloadBlob(bytes: Int8Array, mime: String): Blob = js("new Blob([bytes], { type: mime })")

internal fun download(filename: String, mime: String, buffer: Buffer) {
    val blob = downloadBlob(buffer.readByteArray().toInt8Array(), mime)
    val url = URL.createObjectURL(blob)

    cleaning({
        createElement<HTMLAnchorElement> {
            href = url
            this.download = filename
            click()
        }
    }) {
        window.setTimeout({
            URL.revokeObjectURL(url)
            null
        }, 1000)
    }
}

internal class DownloadUri(override val path: String, private val mime: String) : ImplicitUri {
    override suspend fun <R> read(block: suspend (Source) -> R): R = throw IOException("The browser download does not grant read access")
    override suspend fun write(block: suspend (Sink) -> Unit) {
        val buffer = Buffer()
        block(buffer)
        currentCoroutineContext().ensureActive()
        download(path, mime, buffer)
    }
}
