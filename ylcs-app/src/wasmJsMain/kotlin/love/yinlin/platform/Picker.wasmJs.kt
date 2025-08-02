@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.platform

import kotlinx.browser.document
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import love.yinlin.common.ArrayBufferSource
import love.yinlin.data.MimeType
import love.yinlin.extension.Sources
import love.yinlin.extension.safeToSources
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.toInt8Array
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.Worker
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.FileList
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual object Picker {
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

    private fun openFileUpLoadWorker(files: FileList?, block: (JsAny?) -> Unit) {
        val worker = Worker("js/worker/FileUpload.js")
        worker.onmessage = { event -> block(event.data) }
        worker.postMessage(files)
    }

    actual suspend fun pickPicture(): Source? = suspendCoroutine { continuation ->
        continuation.safeResume {
            htmlFileInput(multiple = false, filter = MimeType.IMAGE) { files ->
                continuation.safeResume {
                    openFileUpLoadWorker(files) { data ->
                        continuation.safeResume {
                            val buffer = ((data as JsArray<*>).toArray().getOrNull(0) as ArrayBuffer)
                            continuation.resume(ArrayBufferSource(buffer).buffered())
                        }
                    }
                }
            }
        }
    }

    actual suspend fun pickPicture(maxNum: Int): Sources<Source>? = suspendCoroutine { continuation ->
        continuation.safeResume {
            require(maxNum > 0)
            htmlFileInput(multiple = true, filter = MimeType.IMAGE) { files ->
                continuation.safeResume {
                    openFileUpLoadWorker(files) { data ->
                        continuation.safeResume {
                            continuation.resume((data as? JsArray<*>)?.toList()?.safeToSources {
                                (it as? ArrayBuffer)?.let { buffer -> ArrayBufferSource(buffer).buffered() }
                            })
                        }
                    }
                }
            }
        }
    }

    actual suspend fun pickFile(mimeType: List<String>, filter: List<String>): Source? = suspendCoroutine { continuation ->
        continuation.safeResume {
            htmlFileInput(multiple = false, filter = mimeType.joinToString(",")) { files ->
                continuation.safeResume {
                    openFileUpLoadWorker(files) { data ->
                        continuation.safeResume {
                            val buffer = ((data as JsArray<*>).toArray().getOrNull(0) as ArrayBuffer)
                            continuation.resume(ArrayBufferSource(buffer).buffered())
                        }
                    }
                }
            }
        }
    }

    actual suspend fun pickPath(mimeType: List<String>, filter: List<String>): ImplicitPath? = unsupportedPlatform()

    actual suspend fun savePath(filename: String, mimeType: String, filter: String): ImplicitPath? = unsupportedPlatform()

    actual suspend fun prepareSavePicture(filename: String): Pair<Any, Sink>? {
        val buffer = Buffer()
        return buffer to buffer
    }

    actual suspend fun prepareSaveVideo(filename: String): Pair<Any, Sink>? = prepareSavePicture(filename)

    actual suspend fun actualSave(filename: String, origin: Any, sink: Sink) {
        val blob = Coroutines.io {
            val bytes = (origin as Buffer).readByteArray()
            Blob(listOf(bytes.toInt8Array()).toJsArray(), BlobPropertyBag(type = MimeType.ANY))
        }
        val url = URL.createObjectURL(blob)
        val link = document.createElement("a") as HTMLAnchorElement
        link.href = url
        link.download = filename
        link.click()
    }

    actual suspend fun cleanSave(origin: Any, result: Boolean) = Unit
}