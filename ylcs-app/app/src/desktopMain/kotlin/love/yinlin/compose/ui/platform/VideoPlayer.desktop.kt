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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import love.yinlin.compose.*
import love.yinlin.compose.ui.common.ComposeVideoSurface
import love.yinlin.compose.ui.image.ClickIcon
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.media.Media
import uk.co.caprica.vlcj.media.MediaEventAdapter
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import javax.swing.SwingUtilities

internal val PLAYER_ARGS = arrayOf(
    "--video-title=ylcs video output",
    "--no-snapshot-preview",
    "--quiet",
    "--intf=dummy"
)

@Stable
private class VideoPlayerState(val url: String) {
    var playerFactory by mutableRefStateOf<MediaPlayerFactory?>(null)
    var player by mutableRefStateOf<EmbeddedMediaPlayer?>(null)
    var surface by mutableRefStateOf<ComposeVideoSurface?>(null)

    var isPlaying by mutableStateOf(false)
    var position by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)

    val eventListener = object : MediaEventAdapter() {
        override fun mediaDurationChanged(media: Media?, newDuration: Long) {
            duration = newDuration
        }
    }

    val playerListener = object : MediaPlayerEventAdapter() {
        override fun playing(mediaPlayer: MediaPlayer) { isPlaying = true }
        override fun paused(mediaPlayer: MediaPlayer) { isPlaying = false }
        override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) { position = newTime }
        override fun stopped(mediaPlayer: MediaPlayer) {
            SwingUtilities.invokeLater {
                mediaPlayer.media().play(url)
            }
        }
        override fun error(mediaPlayer: MediaPlayer) { mediaPlayer.controls().stop() }
    }

    fun play() = player?.let { player ->
        if (!player.status().isPlaying) player.controls().play()
    }

    fun pause() = player?.let { player ->
        if (player.status().isPlaying) player.controls().pause()
    }

    fun seekTo(value: Long) = player?.let { player ->
        player.controls().setTime(value)
        play()
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
        state.playerFactory = MediaPlayerFactory(*PLAYER_ARGS)
        state.player = state.playerFactory
                            ?.mediaPlayers()
                            ?.newEmbeddedMediaPlayer()
        state.surface = ComposeVideoSurface()
        state.player?.apply {
            videoSurface().set(state.surface)
            SwingUtilities.invokeLater {
                events().apply {
                    addMediaEventListener(state.eventListener)
                    addMediaPlayerEventListener(state.playerListener)
                }
                media().play(url)
            }
        }
        onDispose {
            state.player?.events()?.apply {
                removeMediaEventListener(state.eventListener)
                removeMediaPlayerEventListener(state.playerListener)
            }
            state.player?.release()
            state.playerFactory?.release()
        }
    }

    OffScreenEffect { isForeground ->
        if (isForeground) state.play()
        else state.pause()
    }

    Box(modifier = modifier) {
        state.surface?.bitmap?.value?.let { bitmap ->
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