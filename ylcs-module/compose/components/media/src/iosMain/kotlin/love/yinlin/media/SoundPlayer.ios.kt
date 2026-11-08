@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package love.yinlin.media

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.mainImmediateContext
import love.yinlin.extension.catching
import love.yinlin.extension.catchingError
import love.yinlin.fs.File
import platform.AVFAudio.*
import platform.Foundation.*
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.darwin.NSObjectProtocol
import kotlin.time.Duration.Companion.milliseconds

actual class SoundPlayer {
    private val lifetime = IOSPlayerLifetime()
    private val bank = MiniaudioSoundBank()
    private val token = IOSAudioSessionPolicy.Token(IOSAudioSessionPolicy.Kind.Effect)
    private val notifications: MutableList<NSObjectProtocol> = []
    private var playbackJob: Job? = null
    private var restore: (suspend () -> Boolean)? = null
    private var sourceVersion = 0L

    private fun stopEffects() {
        playbackJob?.cancel()
        playbackJob = null
        bank.withHandle(Unit, SoundNative::stop)
        IOSAudioSessionPolicy.release(token)
    }

    private fun post(block: () -> Unit) {
        lifetime.scope.launch(mainImmediateContext) {
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
        notifications += center.addObserverForName(AVAudioSessionMediaServicesWereResetNotification, session, NSOperationQueue.mainQueue) { notification ->
            post {
                IOSAudioSessionPolicy.resetDevice(notification)
                stopEffects()
                val version = sourceVersion
                lifetime.scope.launch {
                    if (sourceVersion == version) catching { restore?.invoke() }
                }
            }
        }
        notifications += center.addObserverForName(UIApplicationDidEnterBackgroundNotification, null, NSOperationQueue.mainQueue) {
            post(::stopEffects)
        }
    }

    private suspend fun replace(load: suspend () -> Boolean) = Coroutines.main {
        if (lifetime.isClosed) return@main
        sourceVersion++
        if (load() && !lifetime.isClosed) {
            restore = load
            attachNotifications()
        }
    }

    actual suspend fun loadFromByteArray(data: List<ByteArray>) = replace { bank.loadFromByteArray(data) }
    actual suspend fun loadFromPath(data: List<File>) = replace { bank.loadFromPath(data) }

    actual fun play(index: Int) = post {
        val failure = catchingError {
            IOSAudioSessionPolicy.acquire(token)
            check(bank.play(index)) { "播放音效失败" }
        }
        if (playbackJob?.isActive != true) {
            if (failure != null) stopEffects()
            else playbackJob = lifetime.scope.launch {
                do {
                    delay(50.milliseconds)
                } while (bank.withHandle(false, SoundNative::isPlaying))
                stopEffects()
            }
        }
    }

    actual fun release() {
        lifetime.close {
            stopEffects()
            bank.release()
            restore = null
            notifications.forEach { NSNotificationCenter.defaultCenter.removeObserver(it) }
            notifications.clear()
        }
    }
}
