package love.yinlin.media

import androidx.compose.runtime.Stable
import love.yinlin.annotation.NativeLibApi
import love.yinlin.compose.ui.media.VideoActionBar
import love.yinlin.compose.ui.media.VideoController
import love.yinlin.foundation.Context
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import javax.swing.SwingUtilities

@Stable
@NativeLibApi
internal class WindowsVideoController(context: Context, topBar: VideoActionBar?, bottomBar: VideoActionBar?) : VideoController(context, topBar, bottomBar) {
    @Stable
    private enum class PlaybackState { None, Opening, Buffering, Playing, Paused; }

    external override fun nativeCreate(): Long
    external override fun nativeRelease(handle: Long)

    override fun load(path: String) {
        if (url != path) {
            url = path
            nativeSetSource(nativeHandle, path)
        }
    }

    override fun play() {
        if (nativeGetPlaybackState(nativeHandle) != PlaybackState.Playing.ordinal) nativePlay(nativeHandle)
    }

    override fun pause() {
        if (nativeGetPlaybackState(nativeHandle) == PlaybackState.Playing.ordinal) nativePause(nativeHandle)
    }

    override fun stop() {
        nativeSetSource(nativeHandle, null)
    }

    override fun seek(position: Long) {
        nativeSeek(nativeHandle, position)
        nativePlay(nativeHandle)
    }

    // Callback
    @Suppress("unused")
    private fun nativeDurationChange(duration: Long) {
        this.duration = duration
    }

    @Suppress("unused")
    private fun nativePlaybackStateChange(value: Int) {
        when (value) {
            PlaybackState.Playing.ordinal -> isPlaying = true
            PlaybackState.None.ordinal, PlaybackState.Paused.ordinal -> isPlaying = false
            PlaybackState.Opening.ordinal -> nativePlay(nativeHandle)
        }
    }

    @Suppress("unused")
    private fun nativeMediaEnded() {
        if (url != null) nativeSetSource(nativeHandle, url)
    }

    @Suppress("unused")
    private fun nativeOnError(message: String) {
        error = IllegalStateException(message)
    }

    @Suppress("unused")
    private fun nativeFrameSize(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            SwingUtilities.invokeLater {
                if (!isRelease) {
                    image?.let {
                        if (!it.isClosed) it.close()
                    }
                    image = Bitmap().apply {
                        allocPixels(ImageInfo(width, height, colorType = ColorType.BGRA_8888, alphaType = ColorAlphaType.PREMUL))
                        // https://github.com/coil-kt/coil/pull/2594
                        setImmutable()
                    }
                    updateCount = 0L
                }
            }
        }
    }

    @Suppress("unused")
    private fun nativeUpdateFrame(data: ByteArray) {
        SwingUtilities.invokeLater {
            val currentBitmap = image
            if (!isRelease && currentBitmap != null) {
                currentBitmap.installPixels(data)
                currentBitmap.notifyPixelsChanged()
                position = nativeGetPosition(nativeHandle)
                ++updateCount
            }
        }
    }

    private external fun nativeGetPlaybackState(handle: Long): Int
    private external fun nativeGetPosition(handle: Long): Long
    private external fun nativeSetSource(handle: Long, path: String?)
    private external fun nativePlay(handle: Long)
    private external fun nativePause(handle: Long)
    private external fun nativeSeek(handle: Long, position: Long)
}