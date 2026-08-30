@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("unused")
package love.yinlin.media

import androidx.compose.ui.util.fastMap
import kotlinx.coroutines.await
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.catching
import love.yinlin.extension.then
import love.yinlin.fs.File
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.toInt8Array
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.js

private fun newAudioContext(): JsAny = js("new (window.AudioContext || window.webkitAudioContext)()")
private fun decodeAudioData(context: JsAny, buffer: ArrayBuffer): Promise<JsAny> = js("context.decodeAudioData(buffer)")
private fun playAudioBuffer(context: JsAny, audioBuffer: JsAny): JsAny = js("""
{
    const source = context.createBufferSource();
    source.buffer = audioBuffer;
    source.connect(context.destination);
    source.start(0.0);
}
""")

actual class SoundPlayer {
    private var caches = emptyList<JsAny>()
    private val context = newAudioContext()

    actual suspend fun loadFromByteArray(data: List<ByteArray>) {
        catching {
            caches = Coroutines.io {
                data.fastMap { bytes ->
                    decodeAudioData(context, bytes.toInt8Array().buffer).await()
                }
            }
        }
    }

    actual suspend fun loadFromPath(data: List<File>) { }

    actual fun play(index: Int) {
        caches.getOrNull(index)?.then { buffer -> playAudioBuffer(context, buffer) }
    }

    actual fun release() { }
}