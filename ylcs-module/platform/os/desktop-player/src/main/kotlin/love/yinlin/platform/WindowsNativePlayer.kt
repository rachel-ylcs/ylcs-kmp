package love.yinlin.platform

enum class WindowsNativePlaybackState {
    None, Opening, Buffering, Playing, Paused;

    companion object {
        fun fromInt(value: Int): WindowsNativePlaybackState = when (value) {
            Opening.ordinal -> Opening
            Buffering.ordinal -> Buffering
            Playing.ordinal -> Playing
            Paused.ordinal -> Paused
            else -> None
        }
    }
}

class WindowsNativeAudioPlayer {
    companion object {
        init {
            NativeLibLoader.resource("desktop_player")
        }
    }

    open class Listener {
        open fun onDurationChange(duration: Long) { }
        open fun onPlaybackStateChange(state: WindowsNativePlaybackState) { }
        open fun onSourceChange() { }
        open fun onMediaEnded() { }
        open fun onError(message: String) { }
    }

    private var nativeHandle: Long = 0
    private var listener: Listener? = null

    val isInit: Boolean get() = nativeHandle != 0L

    val playbackState: WindowsNativePlaybackState get() = WindowsNativePlaybackState.fromInt(nativeGetPlaybackState(nativeHandle))

    val position: Long get() = nativeGetPosition(nativeHandle)
    val duration: Long get() = nativeGetDuration(nativeHandle)

    fun create(listener: Listener? = null): Boolean {
        nativeHandle = nativeCreate()
        return if (nativeHandle != 0L) {
            this.listener = listener
            true
        } else false
    }

    fun release() {
        this.listener = null
        nativeRelease(nativeHandle)
        nativeHandle = 0L
    }

    fun load(path: String) = nativeSetSource(nativeHandle, path)

    fun play() {
        if (playbackState != WindowsNativePlaybackState.Playing) nativePlay(nativeHandle)
    }

    fun pause() {
        if (playbackState == WindowsNativePlaybackState.Playing) nativePause(nativeHandle)
    }

    fun stop() {
        nativeSetSource(nativeHandle, null)
    }

    fun seek(position: Long) {
        nativeSeek(nativeHandle, position)
    }

    // Callback
    @Suppress("unused")
    private fun nativeDurationChange(duration: Long) { listener?.onDurationChange(duration) }
    @Suppress("unused")
    private fun nativePlaybackStateChange(value: Int) { listener?.onPlaybackStateChange(WindowsNativePlaybackState.fromInt(value)) }
    @Suppress("unused")
    private fun nativeSourceChange() { listener?.onSourceChange() }
    @Suppress("unused")
    private fun nativeMediaEnded() { listener?.onMediaEnded() }
    @Suppress("unused")
    private fun nativeOnError(message: String) { listener?.onError(message) }

    // Native
    private external fun nativeCreate(): Long
    private external fun nativeRelease(handle: Long)
    private external fun nativeGetPlaybackState(handle: Long): Int
    private external fun nativeGetPosition(handle: Long): Long
    private external fun nativeGetDuration(handle: Long): Long
    private external fun nativeSetSource(handle: Long, path: String?)
    private external fun nativePlay(handle: Long)
    private external fun nativePause(handle: Long)
    private external fun nativeSeek(handle: Long, position: Long)
}

class WindowsNativeVideoPlayer {
    companion object {
        init {
            NativeLibLoader.resource("desktop_player")
        }
    }

    open class Listener {
        open fun onDurationChange(duration: Long) { }
        open fun onPlaybackStateChange(state: WindowsNativePlaybackState) { }
        open fun onSourceChange() { }
        open fun onMediaEnded() { }
        open fun onError(message: String) { }
        open fun onFrame(width: Int, height: Int, data: ByteArray) { }
    }

    private var nativeHandle: Long = 0
    private var listener: Listener? = null

    val isInit: Boolean get() = nativeHandle != 0L

    val playbackState: WindowsNativePlaybackState get() = WindowsNativePlaybackState.fromInt(nativeGetPlaybackState(nativeHandle))

    val position: Long get() = nativeGetPosition(nativeHandle)
    val duration: Long get() = nativeGetDuration(nativeHandle)

    fun create(listener: Listener? = null): Boolean {
        nativeHandle = nativeCreate()
        return if (nativeHandle != 0L) {
            this.listener = listener
            true
        } else false
    }

    fun release() {
        this.listener = null
        nativeRelease(nativeHandle)
        nativeHandle = 0L
    }

    fun load(path: String) = nativeSetSource(nativeHandle, path)

    fun play() {
        if (playbackState != WindowsNativePlaybackState.Playing) nativePlay(nativeHandle)
    }

    fun pause() {
        if (playbackState == WindowsNativePlaybackState.Playing) nativePause(nativeHandle)
    }

    fun stop() {
        nativeSetSource(nativeHandle, null)
    }

    fun seek(position: Long) {
        nativeSeek(nativeHandle, position)
    }

    // Callback
    @Suppress("unused")
    private fun nativeDurationChange(duration: Long) { listener?.onDurationChange(duration) }
    @Suppress("unused")
    private fun nativePlaybackStateChange(value: Int) { listener?.onPlaybackStateChange(WindowsNativePlaybackState.fromInt(value)) }
    @Suppress("unused")
    private fun nativeSourceChange() { listener?.onSourceChange() }
    @Suppress("unused")
    private fun nativeMediaEnded() { listener?.onMediaEnded() }
    @Suppress("unused")
    private fun nativeOnError(message: String) { listener?.onError(message) }
    @Suppress("unused")
    private fun nativeVideoFrameAvailable(width: Int, height: Int, data: ByteArray) { listener?.onFrame(width, height, data) }

    // Native
    private external fun nativeCreate(): Long
    private external fun nativeRelease(handle: Long)
    private external fun nativeGetPlaybackState(handle: Long): Int
    private external fun nativeGetPosition(handle: Long): Long
    private external fun nativeGetDuration(handle: Long): Long
    private external fun nativeSetSource(handle: Long, path: String?)
    private external fun nativePlay(handle: Long)
    private external fun nativePause(handle: Long)
    private external fun nativeSeek(handle: Long, position: Long)
}