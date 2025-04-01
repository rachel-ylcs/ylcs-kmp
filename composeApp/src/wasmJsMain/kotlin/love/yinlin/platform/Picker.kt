package love.yinlin.platform

import io.ktor.utils.io.core.readBytes
import kotlinx.browser.document
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import love.yinlin.common.ArrayBufferSource
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

actual object PicturePicker {
    actual suspend fun pick(): Source? = suspendCoroutine { continuation ->
        continuation.safeResume {
            htmlFileInput(multiple = false, filter = "image/*") { files ->
                continuation.safeResume {
                    val worker = Worker("js/worker/FileUpload.js")
                    worker.onmessage = { event ->
                        continuation.safeResume {
                            val buffer = ((event.data as JsArray<*>).toArray().getOrNull(0) as ArrayBuffer)
                            continuation.resume(ArrayBufferSource(buffer).buffered())
                        }
                    }
                    worker.postMessage(files)
                }
            }
        }
    }

    actual suspend fun pick(maxNum: Int): Sources<Source>? = suspendCoroutine { continuation ->
        continuation.safeResume {
            require(maxNum > 0)
            htmlFileInput(multiple = true, filter = "image/*") { files ->
                continuation.safeResume {
                    val worker = Worker("js/worker/FileUpload.js")
                    worker.onmessage = { event ->
                        continuation.resume((event.data as? JsArray<*>)?.toList()?.safeToSources {
                            (it as? ArrayBuffer)?.let { buffer -> ArrayBufferSource(buffer).buffered() }
                        })
                    }
                    worker.postMessage(files)
                }
            }
        }
    }

    actual suspend fun prepareSave(filename: String): Pair<Any, Sink>? {
        val buffer = Buffer()
        return buffer to buffer
    }

    actual suspend fun actualSave(filename: String, origin: Any, sink: Sink) {
        val blob = Coroutines.io {
            val bytes = (origin as Buffer).readBytes()
            Blob(listOf(bytes.toInt8Array()).toJsArray(), BlobPropertyBag(type = "*/*"))
        }
        val url = URL.createObjectURL(blob)
        val link = document.createElement("a") as HTMLAnchorElement
        link.href = url
        link.download = filename
        link.click()
    }

    actual suspend fun cleanSave(origin: Any, result: Boolean) = Unit
}