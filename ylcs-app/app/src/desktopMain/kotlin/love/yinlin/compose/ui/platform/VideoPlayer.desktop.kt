package love.yinlin.compose.ui.platform

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import love.yinlin.compose.*
import love.yinlin.compose.ui.image.ClickIcon
import love.yinlin.platform.WindowsNativePlaybackState
import love.yinlin.platform.WindowsNativeVideoPlayer

@Stable
private class VideoPlayerState(val url: String) {
    private val controller = WindowsNativeVideoPlayer()

    var isPlaying by mutableStateOf(false)
        private set
    var position by mutableLongStateOf(0L)
        private set
    var duration by mutableLongStateOf(0L)
        private set
    var bitmap: ImageBitmap? by mutableRefStateOf(null)
        private set

    fun init() {
        controller.create(object : WindowsNativeVideoPlayer.Listener() {
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

            override fun onFrame(bitmap: ImageBitmap) {
                this@VideoPlayerState.bitmap = bitmap
                this@VideoPlayerState.position = controller.position
            }
        })
        controller.load(url)
    }

    fun release() {
        controller.release()
        bitmap = null
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
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    onBack: () -> Unit
) {
    val state by rememberRefState { VideoPlayerState(url) }

    DisposableEffect(Unit) {
        state.init()
        onDispose { state.release() }
    }

    OffScreenEffect { isForeground ->
        if (isForeground) state.play()
        else state.pause()
    }

    Box(modifier = modifier) {
        state.bitmap?.let { bitmap ->
            Image(
                bitmap,
                modifier = Modifier.fillMaxSize().background(Color.Black).zIndex(1f),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
            )
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