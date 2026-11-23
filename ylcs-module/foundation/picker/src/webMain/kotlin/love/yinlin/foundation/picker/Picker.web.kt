@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.foundation.picker

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import love.yinlin.data.MimeType
import love.yinlin.extension.catchingError
import love.yinlin.extension.cleaning
import love.yinlin.extension.createElement
import love.yinlin.foundation.PlatformContextProvider
import love.yinlin.fs.FileSystemFileHandle
import love.yinlin.fs.write
import love.yinlin.io.ArrayBufferSource
import love.yinlin.io.Sources
import love.yinlin.uri.ImplicitUri
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toInt8Array
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.Promise

private fun selectedFileBytes(file: File): Promise<ArrayBuffer> = js("file.arrayBuffer()")
private fun downloadBlob(bytes: Int8Array, mime: String): Blob = js("new Blob([bytes], { type: mime })")
private fun hasSavePicker(): Boolean = js("typeof window.showSaveFilePicker === 'function'")
private fun setPickerCancel(input: HTMLInputElement, handler: (() -> Unit)?): Unit = js("{ input.oncancel = handler; }")

private fun showSavePicker(name: String, mime: String, extension: String): Promise<FileSystemFileHandle?> = js("""
    {
        const options = { suggestedName: name };
        if (extension) {
            const accept = {};
            accept[mime] = [extension];
            options.types = [{ accept: accept }];
        }
        return window.showSaveFilePicker(options).catch(function (error) {
            if (error.name === 'AbortError') return null;
            throw error;
        });
    }
""")

private fun String.fileExtension(): String? = removePrefix("*").takeIf { value ->
    value.startsWith('.') && value.length in 2 .. 16 && value.drop(1).all { it.isLetterOrDigit() || it in "._-+" } && !value.endsWith('.')
}

private fun acceptTypes(mimeType: List<String>, filter: List<String>): String = (mimeType.filter { it.isNotBlank() && it != MimeType.ANY } + filter.mapNotNull { it.fileExtension() }).distinct().joinToString(",")

private suspend fun File.openSource(): Source = ArrayBufferSource(selectedFileBytes(this).await()).buffered()

private fun download(filename: String, mime: String, buffer: Buffer) {
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

private class PickedFileUri(private val file: File) : ImplicitUri {
    override val path: String get() = file.name
    override suspend fun <R> read(block: suspend (Source) -> R): R = file.openSource().use { block(it) }
    override suspend fun write(block: suspend (Sink) -> Unit): Unit = throw IOException("The browser grants read access to this file")
}

private class SaveFileUri(private val handle: FileSystemFileHandle) : ImplicitUri {
    override val path: String get() = handle.name
    override suspend fun <R> read(block: suspend (Source) -> R): R = handle.getFile().await().openSource().use { block(it) }
    override suspend fun write(block: suspend (Sink) -> Unit) = handle.write(block)
}

private class DownloadUri(override val path: String, private val mime: String) : ImplicitUri {
    override suspend fun <R> read(block: suspend (Source) -> R): R = throw IOException("The browser download does not grant read access")
    override suspend fun write(block: suspend (Sink) -> Unit) {
        val buffer = Buffer()
        block(buffer)
        currentCoroutineContext().ensureActive()
        download(path, mime, buffer)
    }
}

actual class Picker actual constructor(context: PlatformContextProvider) : AutoCloseable {
    private var activeInput: HTMLInputElement? = null
    private var dismissInput: (() -> Unit)? = null
    private var closed = false

    private suspend fun selectFiles(maxNum: Int, types: String): List<File>? = suspendCancellableCoroutine { continuation ->
        if (closed || dismissInput != null || !continuation.isActive) {
            if (continuation.isActive) continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        val input = createElement<HTMLInputElement> {
            type = "file"
            multiple = maxNum > 1
            accept = types
        }
        val cleanup = {
            input.onchange = null
            setPickerCancel(input, null)
            if (activeInput === input) {
                activeInput = null
                dismissInput = null
            }
        }
        activeInput = input
        dismissInput = {
            cleanup()
            if (continuation.isActive) continuation.resume(null)
        }
        continuation.invokeOnCancellation { cleanup() }
        if (!continuation.isActive) return@suspendCancellableCoroutine
        setPickerCancel(input) { dismissInput?.invoke() }
        input.onchange = {
            val files = input.files
            cleanup()
            if (continuation.isActive) {
                val selected = if (files == null) null else (0 ..< minOf(maxNum, files.length)).mapNotNull(files::item)
                continuation.resume(selected?.takeIf { it.isNotEmpty() })
            }
        }
        val failure = catchingError { input.click() }
        if (failure != null && continuation.isActive) {
            cleanup()
            continuation.resumeWithException(failure)
        }
    }

    actual suspend fun pickPicture(): Source? = selectFiles(1, MimeType.IMAGE)?.firstOrNull()?.openSource()

    actual suspend fun pickPicture(maxNum: Int): Sources<Source>? {
        require(maxNum > 0)
        val files = selectFiles(maxNum, MimeType.IMAGE) ?: return null
        val sources = Sources<Source>()
        var complete = false
        return cleaning({
            for (file in files) sources += file.openSource()
            complete = true
            sources
        }) { if (!complete) sources.close() }
    }

    actual suspend fun pickFile(mimeType: List<String>, filter: List<String>): Source? = selectFiles(1, acceptTypes(mimeType, filter))?.firstOrNull()?.openSource()

    actual suspend fun pickPath(mimeType: List<String>, filter: List<String>): ImplicitUri? = selectFiles(1, acceptTypes(mimeType, filter))?.firstOrNull()?.let(::PickedFileUri)

    actual suspend fun savePath(filename: String, mimeType: String, filter: String): ImplicitUri? {
        if (closed) return null
        val mime = mimeType.takeIf { '/' in it && '*' !in it } ?: MimeType.BINARY
        if (!hasSavePicker()) return DownloadUri(filename, mime)
        return showSavePicker(filename, mime, filter.fileExtension().orEmpty()).await()?.let(::SaveFileUri)
    }

    actual suspend fun prepareSavePicture(filename: String): Pair<Any, Sink>? = Unit to Buffer()

    actual suspend fun prepareSaveVideo(filename: String): Pair<Any, Sink>? = Unit to Buffer()

    actual suspend fun actualSave(filename: String, origin: Any, sink: Sink) = download(filename, MimeType.BINARY, sink as Buffer)

    actual suspend fun cleanSave(origin: Any, result: Boolean) { }

    actual override fun close() {
        closed = true
        dismissInput?.invoke()
    }
}
