package love.yinlin.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import love.yinlin.concurrent.Lock
import love.yinlin.concurrent.atomic
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.ioContext
import love.yinlin.extension.cleaning
import love.yinlin.fs.File

internal class MiniaudioSoundBank {
    private val lock = Lock()
    private val sequence = atomic(0L)
    private var handle = 0L
    private var closed = false

    private suspend fun replace(load: (Long) -> Unit): Boolean {
        val request = sequence.incrementAndGet()
        return Coroutines.io {
            if (lock.synchronized { closed }) return@io false
            val candidate = SoundNative.create()
            check(candidate != 0L) { "Cannot initialize sound player" }

            var retained = false
            cleaning({
                load(candidate)
                currentCoroutineContext().ensureActive()
                val previous = lock.synchronized {
                    if (closed || sequence.value != request) 0L
                    else {
                        val old = handle
                        handle = candidate
                        retained = true
                        old
                    }
                }
                if (previous != 0L) SoundNative.release(previous)
                retained
            }) {
                if (!retained) SoundNative.release(candidate)
            }
        }
    }

    suspend fun loadFromByteArray(data: List<ByteArray>): Boolean = replace { candidate ->
        for (bytes in data) {
            check(bytes.isNotEmpty() && SoundNative.addBytes(candidate, bytes)) { "Cannot decode sound" }
        }
    }

    suspend fun loadFromPath(data: List<File>): Boolean = replace { candidate ->
        for (path in data) {
            check('\u0000' !in path.path && SoundNative.addFile(candidate, path.path)) { "Cannot load sound: $path" }
        }
    }

    fun <T> withHandle(default: T, action: (Long) -> T): T = lock.synchronized {
        if (!closed && handle != 0L) action(handle) else default
    }

    fun play(index: Int): Boolean = withHandle(false) { SoundNative.play(it, index) }

    fun release() {
        val previous = lock.synchronized {
            if (closed) return
            closed = true
            sequence.incrementAndGet()
            val old = handle
            handle = 0L
            old
        }

        if (previous != 0L) {
            CoroutineScope(ioContext).launch {
                SoundNative.release(previous)
            }
        }
    }
}
