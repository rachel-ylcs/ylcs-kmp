package love.yinlin.media

internal expect object SoundNative {
    fun create(): Long
    fun release(handle: Long)
    fun addFile(handle: Long, path: String): Boolean
    fun addBytes(handle: Long, data: ByteArray): Boolean
    fun play(handle: Long, index: Int): Boolean
}
