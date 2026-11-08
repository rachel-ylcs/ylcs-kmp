@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package love.yinlin.media

import kotlinx.coroutines.launch
import love.yinlin.coroutines.mainImmediateContext
import love.yinlin.extension.catchingError
import platform.AVFAudio.*
import platform.Foundation.*
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.darwin.NSObjectProtocol

internal class IOSMiniaudioPlayer(
    private val lifetime: IOSPlayerLifetime,
    mixWithOthers: Boolean = false,
    private val backgroundMusic: Boolean = false,
    private val keepSessionAtEnd: Boolean = false,
    private val onChanged: () -> Unit = {},
    private val onEnded: (Long) -> Unit = {},
) {
    private val backend = AudioBackend(music = true)
    private val token = IOSAudioSessionPolicy.Token(IOSAudioSessionPolicy.Kind.Media, mixWithOthers)
    private val notifications: MutableList<NSObjectProtocol> = []
    private var source: String? = null
    private var loaded = false
    private var hasSession = false
    private var interruptedPlayWanted = false
    private var pendingSeek: Long? = null

    val isInit: Boolean get() = backend.isInit
    val isPlaying: Boolean get() = backend.query().isPlaying
    val position: Long get() = backend.query().position
    val duration: Long get() = backend.query().duration
    val generation: Long get() = backend.revision
    var wantsPlay = false
        private set
    var lastError: Throwable? = null
        private set

    init {
        attachNotifications()
        lifetime.scope.launch {
            for (event in backend.events) {
                if (lifetime.isClosed || event.revision != generation || !loaded) continue
                when (event.kind) {
                    AudioEvent.ENDED -> {
                        if (!wantsPlay || isPlaying) continue
                        if (!keepSessionAtEnd) {
                            wantsPlay = false
                            releaseSession()
                        }
                        onChanged()
                        onEnded(event.revision)
                    }
                    AudioEvent.ERROR -> fail(IllegalStateException(event.message ?: "音频播放失败 (${event.value})"))
                }
            }
        }
    }

    suspend fun init() = backend.init()

    suspend fun load(path: String, playing: Boolean): Boolean {
        if (lifetime.isClosed || !isInit) return false
        if (loaded) backend.pause()
        val request = backend.nextRequest()
        source = path
        loaded = false
        wantsPlay = playing
        interruptedPlayWanted = false
        pendingSeek = null
        lastError = null
        onChanged()

        val failure = catchingError {
            withIOSAudioFile(path) { localPath ->
                check(backend.load(localPath, request)) { "音频解码失败 (${backend.query().error})" }
            }
        }
        backend.checkCurrent(request)
        if (failure != null) {
            fail(failure)
            return false
        }
        loaded = true
        pendingSeek?.let { backend.seek(it) }
        pendingSeek = null
        if (wantsPlay) play() else releaseSession()
        onChanged()
        return lastError == null
    }

    fun play() {
        if (lifetime.isClosed || source == null) return
        interruptedPlayWanted = false
        wantsPlay = true
        if (!loaded) return
        catchingError {
            IOSAudioSessionPolicy.acquire(token)
            hasSession = true
            check(backend.play()) { "音频播放失败 (${backend.query().error})" }
        }?.let(::fail) ?: run {
            lastError = null
            onChanged()
        }
    }

    fun pause() {
        wantsPlay = false
        interruptedPlayWanted = false
        if (loaded) backend.pause()
        releaseSession()
        onChanged()
    }

    fun seek(position: Long, resume: Boolean = true) {
        if (lifetime.isClosed || source == null) return
        if (loaded) backend.seek(position) else pendingSeek = position
        if (resume) play() else onChanged()
    }

    fun stop(clearError: Boolean = true) {
        pause()
        source = null
        loaded = false
        pendingSeek = null
        if (clearError) lastError = null
        backend.stop()
        onChanged()
    }

    fun release() {
        if (loaded) backend.pause()
        backend.release()
        releaseSession()
        notifications.forEach { NSNotificationCenter.defaultCenter.removeObserver(it) }
        notifications.clear()
    }

    fun checkForFailure() {
        if (loaded && lastError == null) {
            val error = backend.query().error
            if (error != 0L) fail(IllegalStateException("读取音频状态失败 ($error)"))
        }
    }

    private fun fail(error: Throwable) {
        if (lifetime.isClosed) return
        lastError = error
        pause()
    }

    private fun releaseSession() {
        if (hasSession) {
            hasSession = false
            IOSAudioSessionPolicy.release(token)
        }
    }

    private fun post(block: () -> Unit) {
        lifetime.scope.launch(mainImmediateContext) {
            if (!lifetime.isClosed) block()
        }
    }

    private fun attachNotifications() {
        val center = NSNotificationCenter.defaultCenter
        val session = AVAudioSession.sharedInstance()
        notifications += center.addObserverForName(AVAudioSessionInterruptionNotification, session, NSOperationQueue.mainQueue) { notification ->
            post {
                val info = notification?.userInfo ?: return@post
                when (info[AVAudioSessionInterruptionTypeKey].iosUnsigned()) {
                    AVAudioSessionInterruptionTypeBegan -> {
                        val resume = backgroundMusic && wantsPlay
                        IOSAudioSessionPolicy.markUnavailable()
                        pause()
                        interruptedPlayWanted = resume
                    }
                    AVAudioSessionInterruptionTypeEnded -> {
                        val options = info[AVAudioSessionInterruptionOptionKey].iosUnsigned() ?: 0UL
                        val resume = interruptedPlayWanted && options and AVAudioSessionInterruptionOptionShouldResume != 0UL
                        interruptedPlayWanted = false
                        if (resume) play()
                    }
                }
            }
        }
        notifications += center.addObserverForName(AVAudioSessionRouteChangeNotification, session, NSOperationQueue.mainQueue) { notification ->
            post {
                val reason = notification?.userInfo?.get(AVAudioSessionRouteChangeReasonKey).iosUnsigned()
                if (reason == AVAudioSessionRouteChangeReasonOldDeviceUnavailable) pause()
            }
        }
        notifications += center.addObserverForName(AVAudioSessionMediaServicesWereResetNotification, session, NSOperationQueue.mainQueue) { notification ->
            post {
                IOSAudioSessionPolicy.resetDevice(notification)
                val previous = source ?: return@post
                val previousPosition = position
                stop()
                val request = generation
                lifetime.scope.launch {
                    if (generation == request && load(previous, playing = false)) seek(previousPosition, resume = false)
                }
            }
        }
        if (!backgroundMusic) {
            notifications += center.addObserverForName(UIApplicationDidEnterBackgroundNotification, null, NSOperationQueue.mainQueue) {
                post(::pause)
            }
        }
    }
}
