@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.media

import kotlinx.coroutines.await
import kotlinx.io.readByteArray
import love.yinlin.extension.catchingNull
import love.yinlin.fs.File
import love.yinlin.fs.browserSnapshot
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.toInt8Array
import org.w3c.dom.HTMLMediaElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import kotlin.js.Promise

private val mediaScheme = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")
private fun mediaArrayBuffer(blob: Blob): Promise<ArrayBuffer> = js("blob.arrayBuffer()")
private fun mediaBlob(bytes: ArrayBuffer): Blob = js("new Blob([bytes])")
@Suppress("RedundantNullableReturnType")
internal fun mediaErrorMessage(element: HTMLMediaElement): String? = js("element.error && element.error.message || null")

internal suspend fun File.readWebMediaBuffer(): ArrayBuffer = browserSnapshot()?.let {
    mediaArrayBuffer(it).await()
} ?: read { it.readByteArray().toInt8Array().buffer }

internal fun HTMLMediaElement.clearWebMedia() {
    pause()
    removeAttribute("src")
    load()
}

internal fun Double.mediaMilliseconds(): Long = if (isFinite()) (this * 1000).toLong().coerceAtLeast(0L) else 0L

internal class WebMediaSource private constructor(val url: String, private val owned: Boolean) {
    companion object {
        private fun fromBlob(blob: Blob) = WebMediaSource(URL.createObjectURL(blob), true)

        suspend fun fromFile(file: File): WebMediaSource = fromBlob(file.browserSnapshot() ?: mediaBlob(file.read { it.readByteArray().toInt8Array().buffer }))

        suspend fun resolve(path: String): WebMediaSource {
            require(path.isNotBlank()) { "Media source is empty" }

            val snapshot = if (mediaScheme.containsMatchIn(path) || path.startsWith("//")) null else catchingNull { File(path).browserSnapshot() }
            return if (snapshot == null) WebMediaSource(path, false) else fromBlob(snapshot)
        }
    }

    fun release() {
        if (owned) URL.revokeObjectURL(url)
    }
}
