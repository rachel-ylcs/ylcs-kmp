package love.yinlin.platform

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.ui.util.fastMap
import kotlinx.io.files.Path
import love.yinlin.extension.catching

actual class SoundPlayer {
    private var caches = emptyList<Int>()
    private val pool = SoundPool.Builder()
        .setMaxStreams(16)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()

    actual suspend fun loadFromByteArray(data: List<ByteArray>) { }

    actual suspend fun loadFromPath(data: List<Path>) {
        catching {
            caches = Coroutines.io {
                data.fastMap { path ->
                    pool.setOnLoadCompleteListener(null)
                    Coroutines.sync { future ->
                        pool.setOnLoadCompleteListener { _, sampleId, status ->
                            if (status == 0) future.send(sampleId)
                            else future.send()
                        }
                        pool.load(path.toString(), 1)
                    }!!
                }
            }
        }
    }

    actual fun play(index: Int) {
        caches.getOrNull(index)?.let { soundId ->
            pool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    actual fun release() {
        for (soundId in caches) {
            pool.stop(soundId)
            pool.unload(soundId)
        }
        pool.release()
    }
}