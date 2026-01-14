package love.yinlin.platform

import androidx.compose.ui.util.fastMap
import kotlinx.io.files.Path
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.catching
import java.io.ByteArrayInputStream
import java.io.File
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

actual class SoundPlayer {
    private var caches = emptyList<Clip>()

    actual suspend fun loadFromByteArray(data: List<ByteArray>) {
        catching {
            caches = Coroutines.io {
                data.fastMap { bytes ->
                    AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes)).use { stream ->
                        AudioSystem.getClip().also { it.open(stream) }
                    }
                }
            }
        }
    }

    actual suspend fun loadFromPath(data: List<Path>) {
        catching {
            caches = Coroutines.io {
                data.fastMap { path ->
                    AudioSystem.getAudioInputStream(File(path.toString())).use { stream ->
                        AudioSystem.getClip().also { it.open(stream) }
                    }
                }
            }
        }
    }

    actual fun play(index: Int) {
        caches.getOrNull(index)?.let { clip ->
            clip.framePosition = 0
            clip.start()
        }
    }

    actual fun release() {
        for (clip in caches) {
            clip.stop()
            clip.close()
        }
        caches = emptyList()
    }
}