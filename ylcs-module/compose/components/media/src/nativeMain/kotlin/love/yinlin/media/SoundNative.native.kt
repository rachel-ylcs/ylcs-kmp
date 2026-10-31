@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.media

import kotlinx.cinterop.*
import love.yinlin.media.internal.*

internal actual object SoundNative {
    actual fun create(): Long = rachel_sound_create()?.rawValue?.toLong() ?: 0L
    actual fun release(handle: Long) = rachel_sound_release(handle.toCPointer<CPointed>())
    actual fun addFile(handle: Long, path: String): Boolean = rachel_sound_add_file(handle.toCPointer<CPointed>(), path)
    actual fun addBytes(handle: Long, data: ByteArray): Boolean = data.isNotEmpty() && data.usePinned { rachel_sound_add_memory(handle.toCPointer<CPointed>(), it.addressOf(0), data.size.toLong()) }
    actual fun play(handle: Long, index: Int): Boolean = rachel_sound_play(handle.toCPointer<CPointed>(), index)
}
