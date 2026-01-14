package love.yinlin.io

import org.w3c.dom.Worker
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.toJsArray
import kotlin.js.toJsString

@OptIn(ExperimentalWasmJsInterop::class)
class ScriptWorker(script: String) {
    private val text = """
        onmessage = function(event) {
            const data = event.data;
            $script
            close();
        };
    """

    val url = URL.createObjectURL(
        Blob(
            blobParts = listOf(text.toJsString()).toJsArray(),
            options = BlobPropertyBag(type = "application/javascript")
        )
    )

    inline fun execute(data: JsAny? = null, crossinline block: (data: JsAny?) -> Unit) {
        val worker = Worker(url)
        worker.onmessage = { event -> block(event.data) }
        worker.postMessage(data)
    }
}