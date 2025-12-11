@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.platform

import androidx.compose.ui.util.fastMap
import kotlinx.io.files.Path
import love.yinlin.extension.catching
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.toInt8Array

private fun newAudioContext(): JsAny = js("new (window.AudioContext || window.webkitAudioContext)()")
private fun decodeAudioData(context: JsAny, buffer: ArrayBuffer): JsAny = js("context.decodeAudioData(buffer).await()")
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
                    decodeAudioData(context, bytes.toInt8Array().buffer)
                }
            }
        }
    }

    actual suspend fun loadFromPath(data: List<Path>) { }

    actual fun play(index: Int) {
        caches.getOrNull(index)?.let { buffer -> playAudioBuffer(context, buffer) }
    }

    actual fun release() { }
}