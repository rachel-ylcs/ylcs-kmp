@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package love.yinlin.media

import kotlinx.coroutines.*
import love.yinlin.extension.catchingError
import platform.AVFoundation.*
import platform.AVFAudio.*
import platform.CoreMedia.CMTimeMake
import platform.Foundation.*
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.darwin.NSObjectProtocol
import kotlin.time.Duration.Companion.milliseconds

/**
 * 一个 AVPlayer、一个音源。生命周期和回调 scope 归外层播放器所有。
 * 控制方法在 Main 调用；position/duration 直接读取底层，不保存 Compose state。
 * 本类只为异步准备/待执行 seek 维护短期任务；不定时更新播放进度。
 */
internal class IOSAVPlayer(
    private val lifetime: IOSPlayerLifetime,
    mixWithOthers: Boolean = false,
    private val backgroundMusic: Boolean = false,
    private val keepSessionAtEnd: Boolean = false,
    private val onChanged: () -> Unit = {},
    private val onEnded: (Long) -> Unit = {},
) {
    var nativePlayer: AVPlayer = AVPlayer()
        private set
    val isPlaying: Boolean get() = !disposed && !lifetime.isClosed && wantsPlay && hasSession && !seeking && nativePlayer.timeControlStatus == AVPlayerTimeControlStatusPlaying
    val position: Long get() = if (disposed || lifetime.isClosed || item == null) 0L else nativePlayer.currentTime().iosMilliseconds()
    val duration: Long get() = if (disposed || lifetime.isClosed) 0L else item?.duration?.iosMilliseconds() ?: 0L
    var lastError: Throwable? = null
        private set
    var generation = 0L
        private set
    var wantsPlay = false
        private set

    private val token = IOSAudioSessionPolicy.Token(IOSAudioSessionPolicy.Kind.Media, mixWithOthers)
    private var disposed = false
    private var hasSession = false
    private var interruptedPlayWanted = false
    private var ended = false
    private var source: String? = null
    private var item: AVPlayerItem? = null
    private var seekVersion = 0L
    private var seekTarget: Long? = null
    private var seeking = false
    private var preparationJob: Job? = null
    private val itemNotifications: MutableList<NSObjectProtocol> = []
    private val lifetimeNotifications: MutableList<NSObjectProtocol> = []

    init {
        check(NSThread.isMainThread)
        attachLifetimeNotifications()
    }

    private fun post(block: () -> Unit) {
        lifetime.scope.launch(Dispatchers.Main.immediate) {
            if (!disposed && !lifetime.isClosed) block()
        }
    }

    fun load(path: String, playing: Boolean): Boolean {
        if (disposed || lifetime.isClosed) return false
        val url = iosMediaURL(path)
        if (url == null) {
            stop(clearError = false)
            fail(IllegalArgumentException("不支持的媒体地址: $path"))
            return false
        }

        val result = catchingError {
            if (nativePlayer.status == AVPlayerStatusFailed) replacePlayer()
            generation++
            seekVersion++
            seekTarget = null
            seeking = false
            wantsPlay = false
            interruptedPlayWanted = false
            ended = false
            detachItem()
            nativePlayer.pause()

            source = path
            lastError = null
            val newItem = AVPlayerItem(url)
            item = newItem
            nativePlayer.replaceCurrentItemWithPlayerItem(newItem)
            attachItem(newItem, generation)
            if (!checkForFailure()) return false
            onChanged()
            if (playing) play() else pause()
        }?.let {
            fail(it)
            false
        }
        return result ?: (lastError == null)
    }

    fun play() {
        if (disposed || lifetime.isClosed || item == null) return
        interruptedPlayWanted = false
        if (lastError != null) {
            source?.let { load(it, playing = true) }
            return
        }

        wantsPlay = true
        try {
            IOSAudioSessionPolicy.acquire(token)
            hasSession = true
            startNativePlayback()
            checkForFailure()
            onChanged()
            ensurePreparation()
        } catch (error: Throwable) {
            fail(error)
        }
    }

    fun pause() {
        if (disposed) return
        wantsPlay = false
        interruptedPlayWanted = false
        nativePlayer.pause()
        releaseSession()
        checkForFailure()
        onChanged()
    }

    fun stop(clearError: Boolean = true) {
        if (disposed) return
        generation++
        seekVersion++
        seekTarget = null
        seeking = false
        wantsPlay = false
        interruptedPlayWanted = false
        ended = false
        nativePlayer.pause()
        detachItem()
        nativePlayer.replaceCurrentItemWithPlayerItem(null)
        releaseSession()

        source = null
        if (clearError) lastError = null
        onChanged()
    }

    fun seek(position: Long, resume: Boolean = true) {
        if (disposed || lifetime.isClosed || item == null) return
        seekVersion++
        seeking = false
        seekTarget = clampPosition(position)
        ended = false
        item?.cancelPendingSeeks()
        performPendingSeek()
        ensurePreparation()
        onChanged()
        if (resume) play()
    }

    fun release() {
        if (disposed) return
        disposed = true
        generation++
        seekVersion++
        wantsPlay = false
        interruptedPlayWanted = false
        nativePlayer.pause()
        detachItem()
        lifetimeNotifications.forEach {
            NSNotificationCenter.defaultCenter.removeObserver(it)
        }
        lifetimeNotifications.clear()
        nativePlayer.replaceCurrentItemWithPlayerItem(null)
        releaseSession()
        source = null
        seekTarget = null
        seeking = false
        lastError = null
    }

    private fun releaseSession() {
        if (hasSession) {
            hasSession = false
            IOSAudioSessionPolicy.release(token)
        }
    }

    private fun clampPosition(value: Long): Long {
        val positive = value.coerceAtLeast(0L)
        return if (duration > 0L) positive.coerceAtMost(duration) else positive
    }

    private fun startNativePlayback() {
        if (disposed || lifetime.isClosed || !wantsPlay || !hasSession || lastError != null || seeking || item?.status != AVPlayerItemStatusReadyToPlay) return

        if (ended && seekTarget == null) {
            seekVersion++
            seekTarget = 0L
        }

        if (seekTarget != null) performPendingSeek()
        else if (nativePlayer.timeControlStatus == AVPlayerTimeControlStatusPaused) nativePlayer.play()
    }

    private fun performPendingSeek() {
        val currentItem = item ?: return
        val target = seekTarget ?: return
        if (disposed || lifetime.isClosed || seeking || currentItem.status != AVPlayerItemStatusReadyToPlay) return

        val expectedGeneration = generation
        val expectedSeek = seekVersion
        val player = nativePlayer
        val actualTarget = clampPosition(target)
        seekTarget = actualTarget
        seeking = true
        player.seekToTime(
            time = CMTimeMake(actualTarget, 1000),
            toleranceBefore = CMTimeMake(0L, 1000),
            toleranceAfter = CMTimeMake(0L, 1000),
        ) { finished ->
            post {
                if (generation != expectedGeneration || seekVersion != expectedSeek || player !== nativePlayer) return@post
                seeking = false
                seekTarget = null
                if (finished) {
                    ended = false
                    startNativePlayback()
                    checkForFailure()
                    onChanged()
                    ensurePreparation()
                }
                else if (wantsPlay && hasSession) fail(IllegalStateException("调整播放进度失败"))
                else onChanged()
            }
        }
    }

    /** 控制流程和上层 UI 采样时检测故障；不缓存进度、时长或播放状态。 */
    fun checkForFailure(): Boolean {
        if (disposed || lifetime.isClosed || lastError != null) return false
        val currentItem = item
        when {
            currentItem?.status == AVPlayerItemStatusFailed -> {
                fail(iosError("媒体加载失败", currentItem.error))
                return false
            }
            nativePlayer.status == AVPlayerStatusFailed -> {
                fail(iosError("播放器失败", nativePlayer.error))
                return false
            }
        }
        return true
    }

    private fun fail(error: Throwable) {
        if (disposed || lifetime.isClosed) return
        lastError = error
        wantsPlay = false
        interruptedPlayWanted = false
        nativePlayer.pause()
        // acquire 可能在失败前已经登记过 token，清理不能只依赖 hasSession。
        hasSession = false
        IOSAudioSessionPolicy.release(token)
        onChanged()
    }

    /**
     * KVO 回调不能 override，因此短暂等待 item 准备完成以启动播放或提交 seek。
     * 准备和 seek 完成后退出，即使仍在播放也不继续运行。
     * Music/Video 的 UI 进度采样由各自的 scope 负责；Audio 没有进度采样任务。
     */
    private fun ensurePreparation() {
        if (disposed || lifetime.isClosed || lastError != null || preparationJob?.isActive == true) return
        val observedItem = item ?: return
        val expectedGeneration = generation
        preparationJob = lifetime.scope.launch {
            catchingError {
                while (isActive && !disposed && !lifetime.isClosed && generation == expectedGeneration && item === observedItem) {
                    if (!checkForFailure()) break
                    if (ended && !seeking && seekTarget == null) break

                    performPendingSeek()
                    startNativePlayback()
                    if (!isActive || disposed || lifetime.isClosed || generation != expectedGeneration || item !== observedItem) break

                    val ready = observedItem.status == AVPlayerItemStatusReadyToPlay
                    if (ready && !seeking && seekTarget == null) {
                        onChanged()
                        break
                    }
                    delay(50.milliseconds)
                }
            }?.let { error ->
                if (generation == expectedGeneration && item === observedItem) fail(error)
            }
        }
    }

    private fun attachItem(currentItem: AVPlayerItem, expectedGeneration: Long) {
        val center = NSNotificationCenter.defaultCenter
        itemNotifications += center.addObserverForName(AVPlayerItemDidPlayToEndTimeNotification, currentItem, NSOperationQueue.mainQueue) {
            post {
                if (generation != expectedGeneration || item !== currentItem || ended || !wantsPlay) return@post
                ended = true
                nativePlayer.pause()
                if (!keepSessionAtEnd) {
                    wantsPlay = false
                    releaseSession()
                }
                onChanged()
                onEnded(expectedGeneration)
            }
        }
        itemNotifications += center.addObserverForName(AVPlayerItemFailedToPlayToEndTimeNotification, currentItem, NSOperationQueue.mainQueue) { notification ->
            post {
                if (generation != expectedGeneration || item !== currentItem) return@post
                val error = notification?.userInfo?.get(AVPlayerItemFailedToPlayToEndTimeErrorKey) as? NSError
                fail(iosError("媒体播放失败", error ?: currentItem.error))
            }
        }
        ensurePreparation()
    }

    private fun detachItem() {
        preparationJob?.cancel()
        preparationJob = null
        itemNotifications.forEach { NSNotificationCenter.defaultCenter.removeObserver(it) }
        itemNotifications.clear()
        item?.cancelPendingSeeks()
        item = null
    }

    private fun attachLifetimeNotifications() {
        val center = NSNotificationCenter.defaultCenter
        val session = AVAudioSession.sharedInstance()
        lifetimeNotifications += center.addObserverForName(AVAudioSessionInterruptionNotification, session, NSOperationQueue.mainQueue) { notification ->
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
        lifetimeNotifications += center.addObserverForName(AVAudioSessionRouteChangeNotification, session, NSOperationQueue.mainQueue) { notification ->
            post {
                val reason = notification?.userInfo?.get(AVAudioSessionRouteChangeReasonKey).iosUnsigned()
                if (reason == AVAudioSessionRouteChangeReasonOldDeviceUnavailable) pause()
            }
        }
        lifetimeNotifications += center.addObserverForName(AVAudioSessionMediaServicesWereResetNotification, session, NSOperationQueue.mainQueue) {
            post {
                IOSAudioSessionPolicy.markUnavailable()
                rebuildAfterReset()
            }
        }
        if (!backgroundMusic) {
            lifetimeNotifications += center.addObserverForName(UIApplicationDidEnterBackgroundNotification, null, NSOperationQueue.mainQueue) {
                post(::pause)
            }
        }
    }

    private fun replacePlayer() {
        nativePlayer.pause()
        detachItem()
        nativePlayer.replaceCurrentItemWithPlayerItem(null)
        nativePlayer = AVPlayer()
    }

    private fun rebuildAfterReset() {
        val previousSource = source
        val previousPosition = position
        pause()
        replacePlayer()
        lastError = null
        if (previousSource != null && load(previousSource, playing = false)) seek(previousPosition, resume = false)
        else onChanged()
    }
}
