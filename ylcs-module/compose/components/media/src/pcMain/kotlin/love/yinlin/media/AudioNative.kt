package love.yinlin.media

internal expect object AudioNative {
    fun create(music: Boolean, callback: (Long, Int, Long, String?) -> Unit): Long
    fun release(handle: Long)
    fun load(handle: Long, path: String, revision: Long): Boolean
    fun play(handle: Long, revision: Long): Boolean
    fun pause(handle: Long, revision: Long): Boolean
    fun seek(handle: Long, position: Long, revision: Long): Boolean
    fun stop(handle: Long, revision: Long)
    fun query(handle: Long, revision: Long): AudioSnapshot
}
