@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.startup

import kotlinx.browser.document
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import love.yinlin.Context
import love.yinlin.StartupArgs
import love.yinlin.SyncStartup
import love.yinlin.compatible.ByteArrayCompatible
import love.yinlin.data.MimeType
import love.yinlin.io.ArrayBufferSource
import love.yinlin.io.ScriptWorker
import love.yinlin.io.Sources
import love.yinlin.io.safeToSources
import love.yinlin.platform.Coroutines
import love.yinlin.uri.ImplicitUri
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.FileList
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsArray
import kotlin.js.toJsArray
import kotlin.js.toList

actual class StartupPicker : SyncStartup() {
    actual override fun init(context: Context, args: StartupArgs) {}

    private fun htmlFileInput(
        multiple: Boolean,
        filter: String,
        block: (FileList?) -> Unit
    ) {
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.multiple = multiple
        input.accept = filter
        input.onchange = { block(input.files) }
        input.click()
    }

    private fun openFileUpLoadWorker(files: FileList?, block: (List<ArrayBuffer>) -> Unit) {
        ScriptWorker("""
            if (data && data instanceof FileList && data.length > 0) {
                const reader = new FileReaderSync();
                let items = [];
                for (const file of data) {
                    const buffer = reader.readAsArrayBuffer(file);
                    items.push(buffer);
                }
                postMessage(items);
            }
        """.trimIndent()).execute(files) { data ->
            block((data as? JsArray<*>)?.toList()?.map { it as ArrayBuffer }!!)
        }
    }

    actual suspend fun pickPicture(): Source? = Coroutines.sync { future ->
        future.catching {
            htmlFileInput(multiple = false, filter = MimeType.IMAGE) { files ->
                future.catching {
                    openFileUpLoadWorker(files) { buffers ->
                        future.send { ArrayBufferSource(buffers[0]).buffered() }
                    }
                }
            }
        }
    }

    actual suspend fun pickPicture(maxNum: Int): Sources<Source>? = Coroutines.sync { future ->
        future.catching {
            require(maxNum > 0)
            htmlFileInput(multiple = true, filter = MimeType.IMAGE) { files ->
                future.catching {
                    openFileUpLoadWorker(files) { buffers ->
                        future.send { buffers.safeToSources { ArrayBufferSource(it).buffered() } }
                    }
                }
            }
        }
    }

    actual suspend fun pickFile(mimeType: List<String>, filter: List<String>): Source? = Coroutines.sync { future ->
        future.catching {
            htmlFileInput(multiple = false, filter = mimeType.joinToString(",")) { files ->
                future.catching {
                    openFileUpLoadWorker(files) { buffers ->
                        future.send { ArrayBufferSource(buffers[0]).buffered() }
                    }
                }
            }
        }
    }

    actual suspend fun pickPath(mimeType: List<String>, filter: List<String>): ImplicitUri? = null

    actual suspend fun savePath(filename: String, mimeType: String, filter: String): ImplicitUri? = null

    actual suspend fun prepareSavePicture(filename: String): Pair<Any, Sink>? = Unit to Buffer()

    actual suspend fun prepareSaveVideo(filename: String): Pair<Any, Sink>? = Unit to Buffer()

    actual suspend fun actualSave(filename: String, origin: Any, sink: Sink) {
        val blob = Coroutines.io {
            val bytes = ByteArrayCompatible((sink as Buffer).readByteArray())
            Blob(listOf(bytes.toInt8Array()).toJsArray(), BlobPropertyBag(type = MimeType.ANY))
        }
        val url = URL.createObjectURL(blob)
        val link = document.createElement("a") as HTMLAnchorElement
        link.href = url
        link.download = filename
        link.click()
    }

    actual suspend fun cleanSave(origin: Any, result: Boolean) {}
}