package love.yinlin.compose.ui.platform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.zIndex
import kotlinx.coroutines.runBlocking
import love.yinlin.annotation.NativeLibApi
import love.yinlin.compose.*
import love.yinlin.compose.extension.rememberRefState
import love.yinlin.compose.ui.image.ClickIcon
import love.yinlin.concurrent.Mutex
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.catching
import love.yinlin.platform.WindowsNativePlaybackState
import love.yinlin.platform.WindowsNativeVideoPlayer
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode

@Stable
@NativeLibApi
private class VideoPlayerState(val url: String) {
    @NativeLibApi
    private val controller = WindowsNativeVideoPlayer()

    var isPlaying by mutableStateOf(false)
        private set
    var position by mutableLongStateOf(0L)
        private set
    var duration by mutableLongStateOf(0L)
        private set

    // https://github.com/coil-kt/coil/pull/2594
    var bitmap: Bitmap? = null

    var mutex = Mutex()
    var isRelease: Boolean = false

    fun init() {
        val isInit = controller.create(object : WindowsNativeVideoPlayer.Listener() {
            override fun onDurationChange(duration: Long) {
                this@VideoPlayerState.duration = duration
            }

            override fun onPlaybackStateChange(state: WindowsNativePlaybackState) {
                when (state) {
                    WindowsNativePlaybackState.Playing -> isPlaying = true
                    WindowsNativePlaybackState.Paused, WindowsNativePlaybackState.None -> isPlaying = false
                    WindowsNativePlaybackState.Opening -> controller.play()
                    else -> {}
                }
            }

            override fun onMediaEnded() {
                controller.load(url)
            }

            override fun onFrame(width: Int, height: Int, data: ByteArray) {
                if (mutex.lock() && !isRelease) {
                    // work on dispatcher main
//                    Coroutines.startMain {
//                        catching {
//                            val currentBitmap = bitmap ?: Bitmap().apply {
//                                allocPixels(ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.PREMUL))
//                                setImmutable()
//                                bitmap = this
//                            }
//                            currentBitmap.installPixels(data)
//                            currentBitmap.notifyPixelsChanged()
//                            position = controller.position
//                        }
//                        mutex.unlock()
//                    }
                }
            }
        })
        if (isInit) controller.load(url)
    }

    fun release() {
        runBlocking {
            mutex.with {
                isRelease = true
                controller.release()
                val tmp = bitmap
                bitmap = null
                tmp?.close()
            }
        }
    }

    fun play() {
        if (controller.isInit && controller.playbackState != WindowsNativePlaybackState.Playing) controller.play()
    }

    fun pause() {
        if (controller.isInit && controller.playbackState == WindowsNativePlaybackState.Playing) controller.pause()
    }

    fun seekTo(value: Long) {
        if (controller.isInit) {
            controller.seek(value)
            controller.play()
        }
    }
}

@Composable
@NativeLibApi
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    onBack: () -> Unit
) {
    val state by rememberRefState { VideoPlayerState(url) }
    val paint = remember { Paint().apply { isAntiAlias = true } }

    DisposableEffect(Unit) {
        state.init()
        onDispose { state.release() }
    }

    OffScreenEffect { isForeground ->
        if (isForeground) state.play()
        else state.pause()
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize().background(Colors.Black).zIndex(1f)) {
            val bitmap = state.bitmap
            // 必须用 state.position 变化来诱导 Compose 重组, 不能放在 && 后面
            if (state.position > 0L && bitmap != null) {
                val canvasWidth = this.size.width
                val canvasHeight = this.size.height
                val canvasRatio = canvasWidth / canvasHeight
                val imageWidth = bitmap.width.toFloat()
                val imageHeight = bitmap.height.toFloat()
                val imageRatio = imageWidth / imageHeight
                Image.makeFromBitmap(bitmap).use { image ->
                    val dst = if (imageRatio > canvasRatio) {
                        val dstHeight = canvasWidth / imageRatio
                        Rect.makeXYWH(0f, (canvasHeight - dstHeight) / 2, canvasWidth, dstHeight)
                    } else {
                        val dstWidth = canvasHeight * imageRatio
                        Rect.makeXYWH((canvasWidth - dstWidth) / 2, 0f, dstWidth, canvasHeight)
                    }
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawImageRect(
                            image = image,
                            src = Rect.makeWH(imageWidth, imageHeight),
                            dst = dst,
                            samplingMode = SamplingMode.MITCHELL,
                            paint = paint,
                            strict = true
                        )
                    }
                }
            }
        }

        VideoPlayerControls(
            modifier = Modifier.fillMaxSize().zIndex(2f),
            isPlaying = state.isPlaying,
            onPlayClick = {
                if (state.isPlaying) state.pause()
                else state.play()
            },
            position = state.position,
            duration = state.duration,
            onProgressClick = { state.seekTo(it) },
            topBar = {
                ClickIcon(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    color = Colors.White,
                    onClick = onBack
                )
            }
        )
    }
}