@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compose.image.input

import kotlinx.coroutines.await
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import love.yinlin.extension.asByteArray
import love.yinlin.fs.File
import love.yinlin.fs.browserSnapshot
import org.khronos.webgl.ArrayBuffer
import org.w3c.files.Blob
import kotlin.js.Promise

private fun imageArrayBuffer(blob: Blob): Promise<ArrayBuffer> = js("blob.arrayBuffer()")

internal suspend fun Blob.readImageBytes(): ByteArray {
    val data = imageArrayBuffer(this).await()
    currentCoroutineContext().ensureActive()
    return data.asByteArray
}

internal suspend fun File.imageSnapshot(maxBytes: Int): Blob? = browserSnapshot()?.also {
    require(it.size.toDouble() in 1.0 .. maxBytes.toDouble()) { "Image file is empty or exceeds the byte budget" }
}

internal actual suspend fun readPlatformImageFile(file: File, maxBytes: Int): ByteArray? = file.imageSnapshot(maxBytes)?.readImageBytes()
