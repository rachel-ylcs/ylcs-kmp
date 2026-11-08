package love.yinlin.media

import love.yinlin.fs.File

actual class SoundPlayer {
    private val bank = MiniaudioSoundBank()

    actual suspend fun loadFromByteArray(data: List<ByteArray>) { bank.loadFromByteArray(data) }
    actual suspend fun loadFromPath(data: List<File>) { bank.loadFromPath(data) }
    actual fun play(index: Int) { bank.play(index) }
    actual fun release() = bank.release()
}
