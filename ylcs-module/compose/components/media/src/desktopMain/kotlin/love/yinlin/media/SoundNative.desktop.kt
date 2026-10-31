package love.yinlin.media

import love.yinlin.foundation.LibLoader
import love.yinlin.foundation.NativeLibApi

@NativeLibApi
internal actual object SoundNative {
    init {
        LibLoader.resource("ylcs-module.compose.components.media")
    }

    actual external fun create(): Long
    actual external fun release(handle: Long)
    private external fun addPath(handle: Long, path: ByteArray): Boolean
    actual fun addFile(handle: Long, path: String): Boolean = addPath(handle, path.encodeToByteArray())
    actual external fun addBytes(handle: Long, data: ByteArray): Boolean
    actual external fun play(handle: Long, index: Int): Boolean
}
