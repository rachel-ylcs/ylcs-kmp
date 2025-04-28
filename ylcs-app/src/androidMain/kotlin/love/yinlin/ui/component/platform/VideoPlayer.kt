@file:JvmName("AndroidVideoPlayer")
package love.yinlin.ui.component.platform

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.compose.LocalActivity
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import love.yinlin.common.Colors
import love.yinlin.common.FfmpegRenderersFactory
import love.yinlin.extension.OffScreenEffect
import love.yinlin.extension.rememberState
import love.yinlin.platform.Coroutines
import love.yinlin.platform.MusicFactory
import love.yinlin.ui.component.image.ClickIcon

private class VideoPlayerState {
    var controller by mutableStateOf<Player?>(null)

    var isPlaying by mutableStateOf(false)
    var position by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)

    var updateProgressJob: Job? = null
    val updateProgressJobLock = Any()

    val listener = object : Player.Listener {
        override fun onIsPlayingChanged(value: Boolean) {
            withPlayer { player ->
                isPlaying = value
                synchronized(updateProgressJobLock) {
                    updateProgressJob?.cancel()
                    updateProgressJob = if (value) Coroutines.startMain {
                        while (true) {
                            if (!isActive) break
                            position = player.currentPosition
                            delay(MusicFactory.UPDATE_INTERVAL)
                        }
                    } else null
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            withPlayer { player ->
                if (playbackState == Player.STATE_READY) duration = player.duration
            }
        }
    }

    inline fun withPlayer(block: (Player) -> Unit) {
        controller?.let(block)
    }

    fun play() = withPlayer {
        if (!it.isPlaying) it.play()
    }

    fun pause() = withPlayer {
        if (it.isPlaying) it.pause()
    }

    fun seekTo(value: Long) = withPlayer {
        it.seekTo(value)
        play()
    }
}

@SuppressLint("SourceLockedOrientationActivity")
@OptIn(UnstableApi::class)
@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by rememberState { VideoPlayerState() }

    val activity = LocalActivity.current!!
    val oldOrientation = remember { activity.requestedOrientation }
    val oldBehavior = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) activity.window.insetsController?.systemBarsBehavior else null
    }

    var isPortrait by rememberState { true }
    LaunchedEffect(isPortrait) {
        if (isPortrait) {
            if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        else {
            if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    DisposableEffect(Unit) {
        state.controller = FfmpegRenderersFactory.build(context, false).apply {
            repeatMode = Player.REPEAT_MODE_ONE
            addListener(state.listener)
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            play()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose {
            synchronized(state.updateProgressJobLock) {
                state.updateProgressJob?.cancel()
            }
            state.controller?.removeListener(state.listener)
            state.controller?.release()
            state.controller = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && oldBehavior != null) {
                activity.window.insetsController?.apply {
                    show(WindowInsets.Type.statusBars())
                    systemBarsBehavior = oldBehavior
                }
            }
            if (activity.requestedOrientation != oldOrientation) activity.requestedOrientation = oldOrientation
        }
    }

    OffScreenEffect { isForeground ->
        if (isForeground) state.play()
        else state.pause()
    }

    Box(modifier = modifier) {
        Box(Modifier.fillMaxSize().background(Colors.Black).zIndex(1f))
        state.controller?.let { player ->
            val presentationState = rememberPresentationState(player)
            val scaledModifier = Modifier.resizeWithContentScale(ContentScale.Inside, presentationState.videoSizeDp)

            PlayerSurface(
                player = player,
                surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                modifier = scaledModifier.zIndex(2f)
            )

            VideoPlayerControls(
                modifier = Modifier.fillMaxSize().zIndex(3f),
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
                },
                rightAction = {
                    ClickIcon(
                        icon = if (isPortrait) Icons.Outlined.Fullscreen else Icons.Outlined.FullscreenExit,
                        color = Colors.White,
                        onClick = { isPortrait = !isPortrait }
                    )
                }
            )
        }
    }
}