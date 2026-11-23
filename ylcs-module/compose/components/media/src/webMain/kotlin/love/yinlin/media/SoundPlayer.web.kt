@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.media

import kotlinx.coroutines.await
import love.yinlin.extension.catching
import love.yinlin.fs.File
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.toInt8Array
import kotlin.js.Promise

private fun newAudioContext(): JsAny = js("new (window.AudioContext || window.webkitAudioContext)()")
private fun decodeAudioData(context: JsAny, buffer: ArrayBuffer): Promise<JsAny> = js("context.decodeAudioData(buffer)")
private fun playAudioBuffer(context: JsAny, audioBuffer: JsAny): Promise<JsAny?> = js("""
    context.resume().then(function () {
        if (context.state !== 'running') return null;
        const source = context.createBufferSource();
        source.buffer = audioBuffer;
        source.connect(context.destination);
        source.onended = function () { source.disconnect(); };
        source.start();
        return null;
    })
""")

private fun closeAudioContext(context: JsAny): Promise<JsAny?> = js("context.close()")

actual class SoundPlayer {
    private var caches: List<JsAny> = []
    private var context: JsAny? = null
    private var revision = 0L
    private var released = false

    private suspend fun load(count: Int, read: suspend (Int) -> ArrayBuffer) {
        if (released) return

        val request = ++revision
        catching {
            val decoded = ArrayList<JsAny>(count)
            if (count > 0) {
                val active = context ?: newAudioContext().also { context = it }
                for (index in 0 ..< count) {
                    val bytes = read(index)
                    if (request != revision) return@catching
                    decoded += decodeAudioData(active, bytes).await()
                    if (request != revision) return@catching
                }
            }
            caches = decoded
        }
    }

    actual suspend fun loadFromByteArray(data: List<ByteArray>) = load(data.size) { data[it].toInt8Array().buffer }

    actual suspend fun loadFromPath(data: List<File>) = load(data.size) { data[it].readWebMediaBuffer() }

    actual fun play(index: Int) {
        val active = context ?: return
        val buffer = caches.getOrNull(index) ?: return
        val _ = playAudioBuffer(active, buffer).catch { null }
    }

    actual fun release() {
        released = true
        revision++
        caches = []
        val active = context ?: return
        context = null
        val _ = closeAudioContext(active).catch { null }
    }
}
