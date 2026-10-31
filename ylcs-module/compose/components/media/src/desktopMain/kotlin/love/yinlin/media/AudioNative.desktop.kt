package love.yinlin.media

import love.yinlin.foundation.LibLoader
import love.yinlin.foundation.NativeLibApi
import java.util.function.BiConsumer

@NativeLibApi
internal actual object AudioNative {
    init {
        LibLoader.resource("ylcs-module.compose.components.media")
    }

    private val snapshots = ThreadLocal.withInitial { LongArray(4) }
    private external fun createPlayer(music: Boolean, callback: BiConsumer<LongArray, ByteArray?>): Long

    actual fun create(music: Boolean, callback: (Long, Int, Long, String?) -> Unit): Long = createPlayer(music) { event, message ->
        callback(event[0], event[1].toInt(), event[2], message?.decodeToString())
    }

    actual external fun release(handle: Long)
    private external fun loadSource(handle: Long, path: ByteArray, revision: Long): Boolean
    actual fun load(handle: Long, path: String, revision: Long): Boolean = loadSource(handle, path.encodeToByteArray(), revision)
    actual external fun play(handle: Long, revision: Long): Boolean
    actual external fun pause(handle: Long, revision: Long): Boolean
    actual external fun seek(handle: Long, position: Long, revision: Long): Boolean
    actual external fun stop(handle: Long, revision: Long)
    private external fun read(handle: Long, revision: Long, snapshot: LongArray)

    actual fun query(handle: Long, revision: Long): AudioSnapshot {
        val value = snapshots.get()
        read(handle, revision, value)
        return AudioSnapshot(value[0] != 0L, value[1], value[2], value[3])
    }
}
