package love.yinlin.media

import kotlinx.io.files.Path

expect class SoundPlayer() {
    suspend fun loadFromByteArray(data: List<ByteArray>)
    suspend fun loadFromPath(data: List<Path>)
    fun play(index: Int)
    fun release()
}