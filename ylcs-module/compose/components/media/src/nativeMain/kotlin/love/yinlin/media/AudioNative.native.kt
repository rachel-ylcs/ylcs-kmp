@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.media

import kotlinx.cinterop.*
import love.yinlin.media.internal.*

internal actual object AudioNative {
    actual fun create(music: Boolean, callback: (Long, Int, Long, String?) -> Unit): Long = createPlayer(music, false, callback)
    fun createVideo(callback: (Long, Int, Long, String?) -> Unit): Long = createPlayer(music = true, video = true, callback = callback)

    private fun createPlayer(music: Boolean, video: Boolean, callback: (Long, Int, Long, String?) -> Unit): Long {
        val context = StableRef.create(callback)
        val listener = staticCFunction { data: COpaquePointer?, event: CPointer<RachelAudioEvent>? ->
            if (data != null && event != null) {
                val value = event.pointed
                data.asStableRef<(Long, Int, Long, String?) -> Unit>().get().invoke(value.revision, value.kind, value.value, value.message?.toKString())
            }
        }
        val release = staticCFunction { data: COpaquePointer? ->
            data?.asStableRef<(Long, Int, Long, String?) -> Unit>()?.dispose() ?: Unit
        }
        val handle = if (video) rachel_video_create(listener, release, context.asCPointer()) else rachel_audio_create(music, listener, release, context.asCPointer())
        return handle?.rawValue?.toLong() ?: 0L
    }
    actual fun release(handle: Long) = rachel_audio_release(handle.toCPointer<CPointed>())
    actual fun load(handle: Long, path: String, revision: Long): Boolean = rachel_audio_load(handle.toCPointer<CPointed>(), path, revision)
    actual fun play(handle: Long, revision: Long): Boolean = rachel_audio_play(handle.toCPointer<CPointed>(), revision)
    actual fun pause(handle: Long, revision: Long): Boolean = rachel_audio_pause(handle.toCPointer<CPointed>(), revision)
    actual fun seek(handle: Long, position: Long, revision: Long): Boolean = rachel_audio_seek(handle.toCPointer<CPointed>(), position, revision)
    actual fun stop(handle: Long, revision: Long) = rachel_audio_stop(handle.toCPointer<CPointed>(), revision)
    actual fun query(handle: Long, revision: Long): AudioSnapshot = memScoped {
        val value = alloc<RachelAudioSnapshot>()
        rachel_audio_query(handle.toCPointer<CPointed>(), revision, value.ptr)
        AudioSnapshot(value.playing, value.position, value.duration, value.error)
    }
}
