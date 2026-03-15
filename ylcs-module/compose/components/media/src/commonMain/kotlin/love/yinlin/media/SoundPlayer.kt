package love.yinlin.media

import love.yinlin.fs.File

expect class SoundPlayer() {
    suspend fun loadFromByteArray(data: List<ByteArray>)
    suspend fun loadFromPath(data: List<File>)
    fun play(index: Int)
    fun release()
}