package love.yinlin.io

import love.yinlin.extension.cast
import love.yinlin.extension.jsArrayOf
import org.w3c.dom.Worker
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

@OptIn(ExperimentalWasmJsInterop::class)
class ScriptWorker(script: String) {
    private val text = """
onmessage = function(event) {
    const data = event.data;
    $script
    close();
};
    """.trimIndent()

    val url = URL.createObjectURL(
        Blob(
            blobParts = jsArrayOf(text).cast(),
            options = BlobPropertyBag(type = "application/javascript")
        )
    )

    inline fun execute(data: JsAny? = null, crossinline block: (data: JsAny?) -> Unit) {
        val worker = Worker(url)
        worker.onmessage = { event -> block(event.data) }
        worker.postMessage(data)
    }
}