@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.foundation.picker

import kotlinx.coroutines.await
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.Source
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.MimeType
import love.yinlin.extension.catchingError
import love.yinlin.extension.cleaning
import love.yinlin.extension.createElement
import love.yinlin.foundation.PlatformContextProvider
import love.yinlin.fs.FileSystemFileHandle
import love.yinlin.fs.openSource
import love.yinlin.io.Sources
import love.yinlin.uri.ImplicitUri
import love.yinlin.uri.WebFileUri
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File
import kotlin.js.Promise


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

actual class Picker actual constructor(context: PlatformContextProvider) : AutoCloseable {
    private var activeInput: HTMLInputElement? = null
    private var dismissInput: (() -> Unit)? = null
    private var closed = false

    private suspend fun selectFiles(maxNum: Int, types: String): List<File>? = Coroutines.sync { future ->
        if (closed || dismissInput != null) {
            future.send()
            return@sync
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

        val dismiss = {
            cleanup()
            future.send()
        }
        dismissInput = dismiss

        future.clean { cleanup() }

        if (activeInput !== input) return@sync

        setPickerCancel(input, dismiss)

        input.onchange = {
            val files = input.files
            cleanup()
            val selected = if (files == null) null else (0 ..< minOf(maxNum, files.length)).mapNotNull(files::item)
            future.send(selected?.takeIf { it.isNotEmpty() })
        }

        if (catchingError { input.click() } != null) dismiss()
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

    actual suspend fun pickPath(mimeType: List<String>, filter: List<String>): ImplicitUri? = selectFiles(1, acceptTypes(mimeType, filter))?.firstOrNull()?.let(::WebFileUri)

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
