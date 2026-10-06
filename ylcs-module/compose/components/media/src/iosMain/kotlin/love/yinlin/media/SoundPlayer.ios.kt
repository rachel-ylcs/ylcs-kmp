@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
package love.yinlin.media

import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.catching
import love.yinlin.extension.catchingError
import love.yinlin.extension.toNSData
import love.yinlin.fs.File
import platform.AVFAudio.*
import platform.Foundation.*
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.darwin.*

@OptIn(ExperimentalForeignApi::class)
actual class SoundPlayer {
    private class Delegate(private val finished: (AVAudioPlayer) -> Unit) : NSObject(), AVAudioPlayerDelegateProtocol {
        override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) = finished(player)
        override fun audioPlayerDecodeErrorDidOccur(player: AVAudioPlayer, error: NSError?) = finished(player)
    }

    private sealed interface Source {
        class Bytes(val data: NSData) : Source
        class Path(val url: NSURL) : Source
    }

    private val lifetime = IOSPlayerLifetime()
    private val scope = lifetime.scope
    private val token = IOSAudioSessionPolicy.Token(IOSAudioSessionPolicy.Kind.Effect)
    private var sources: List<Source> = []
    private var players: List<AVAudioPlayer> = []
    private var delegate: Delegate? = null
    private val playing: MutableSet<AVAudioPlayer> = []
    private val notifications: MutableList<NSObjectProtocol> = []

    private fun createPlayer(source: Source): AVAudioPlayer = memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        error.value = null
        val player = when (source) {
            is Source.Bytes -> AVAudioPlayer(source.data, error.ptr)
            is Source.Path -> AVAudioPlayer(source.url, error.ptr)
        }
        error.value?.let { throw iosError("加载音效失败", it) }
        player
    }

    private fun finish(player: AVAudioPlayer) {
        playing.remove(player)
        if (playing.isEmpty()) IOSAudioSessionPolicy.release(token)
    }

    private fun stopEffects() {
        players.forEach { it.stop() }
        playing.clear()
        IOSAudioSessionPolicy.release(token)
    }

    private fun disposePlayers() {
        stopEffects()
        players.forEach { it.delegate = null }
        players = emptyList()
        delegate = null
    }

    private fun post(block: () -> Unit) {
        scope.launch(Dispatchers.Main.immediate) {
            if (!lifetime.isClosed) block()
        }
    }

    private fun attachNotifications() {
        if (notifications.isNotEmpty()) return
        val center = NSNotificationCenter.defaultCenter
        val session = AVAudioSession.sharedInstance()
        notifications += center.addObserverForName(AVAudioSessionInterruptionNotification, session, NSOperationQueue.mainQueue) { notification ->
            post {
                val type = notification?.userInfo?.get(AVAudioSessionInterruptionTypeKey).iosUnsigned()
                if (type == AVAudioSessionInterruptionTypeBegan) {
                    IOSAudioSessionPolicy.markUnavailable()
                    stopEffects()
                }
            }
        }
        notifications += center.addObserverForName(AVAudioSessionRouteChangeNotification, session, NSOperationQueue.mainQueue) { notification ->
            post {
                val reason = notification?.userInfo?.get(AVAudioSessionRouteChangeReasonKey).iosUnsigned()
                if (reason == AVAudioSessionRouteChangeReasonOldDeviceUnavailable) stopEffects()
            }
        }
        notifications += center.addObserverForName(AVAudioSessionMediaServicesWereResetNotification, session, NSOperationQueue.mainQueue) {
            post {
                IOSAudioSessionPolicy.markUnavailable()
                val previous = sources
                disposePlayers()
                // reset 后重建原生对象，但不重放刚才的音效。
                catching { replaceSources(previous) }
            }
        }
        notifications += center.addObserverForName(UIApplicationDidEnterBackgroundNotification, null, NSOperationQueue.mainQueue) {
            post { stopEffects() }
        }
    }

    private fun replaceSources(newSources: List<Source>) {
        // 新数据全部可解码后才替换旧缓存；加载失败保留原有音效。
        val candidates: MutableList<AVAudioPlayer> = []
        catchingError {
            newSources.forEach { candidates += createPlayer(it) }
        }?.let { failure ->
            candidates.forEach { it.stop() }
            throw failure
        }
        disposePlayers()
        sources = newSources
        delegate = Delegate { player ->
            scope.launch(Dispatchers.Main.immediate) {
                if (!lifetime.isClosed && player in players && !player.isPlaying()) {
                    finish(player)
                }
            }
        }
        players = candidates
        players.forEach { it.delegate = delegate }
        attachNotifications()
    }

    actual suspend fun loadFromByteArray(data: List<ByteArray>) {
        if (lifetime.isClosed || data.all { it.isNotEmpty() }) return
        Coroutines.main {
            replaceSources(data.map { Source.Bytes(it.toNSData()) })
        }
    }

    actual suspend fun loadFromPath(data: List<File>) {
        if (lifetime.isClosed) return
        Coroutines.main {
            replaceSources(data.map { Source.Path(iosMediaURL(it.path)!!) })
        }
    }

    actual fun play(index: Int) {
        scope.launch(Dispatchers.Main.immediate) {
            if (lifetime.isClosed) return@launch
            val player = players.getOrNull(index) ?: return@launch
            // 同一索引重播，不同索引可以同时播放。
            player.stop()
            player.currentTime = 0.0
            playing += player
            catchingError {
                IOSAudioSessionPolicy.acquire(token)
                check(!lifetime.isClosed && player.prepareToPlay() && !lifetime.isClosed && player.play()) { "播放音效失败" }
            }?.let {
                player.stop()
                finish(player)
            }
        }
    }

    actual fun release() {
        lifetime.close {
            disposePlayers()
            sources = emptyList()
            notifications.forEach { NSNotificationCenter.defaultCenter.removeObserver(it) }
            notifications.clear()
        }
    }
}