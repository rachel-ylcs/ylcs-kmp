package love.yinlin.media

import androidx.compose.ui.util.fastMap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.catching
import love.yinlin.extension.toNSData
import platform.AVFAudio.*
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
actual class SoundPlayer {
    private var caches = emptyList<AVAudioPlayer>()

    init {
        AVAudioSession.sharedInstance().apply {
            setCategory(AVAudioSessionCategoryPlayback, error = null)
            setActive(true, error = null)
        }
    }

    actual suspend fun loadFromByteArray(data: List<ByteArray>) {
        catching {
            caches = Coroutines.cpu {
                data.fastMap { bytes ->
                    AVAudioPlayer(data = bytes.toNSData(), error = null).also {
                        it.prepareToPlay()
                    }
                }
            }
        }
    }

    actual suspend fun loadFromPath(data: List<Path>) {
        catching {
            caches = Coroutines.cpu {
                data.fastMap { path ->
                    AVAudioPlayer(NSURL.URLWithString(path.toString())!!, error = null).also {
                        it.prepareToPlay()
                    }
                }
            }
        }
    }

    actual fun play(index: Int) {
        caches.getOrNull(index)?.play()
    }

    actual fun release() {
        for (player in caches) {
            if (player.isPlaying()) player.stop()
        }
    }
}