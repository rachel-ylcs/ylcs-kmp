@file:JvmName("DesktopVideoPlayer")
package love.yinlin.ui.component.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import love.yinlin.common.Colors
import love.yinlin.extension.OffScreenEffect
import love.yinlin.extension.rememberState
import love.yinlin.ui.component.CustomUI
import love.yinlin.ui.component.image.ClickIcon
import uk.co.caprica.vlcj.media.Media
import uk.co.caprica.vlcj.media.MediaEventAdapter
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import javax.swing.SwingUtilities

private class VideoPlayerState(val url: String) {
    var component = mutableStateOf<CallbackMediaPlayerComponent?>(null)

    var isPlaying by mutableStateOf(false)
    var position by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)

    val eventListener = object : MediaEventAdapter() {
        override fun mediaDurationChanged(media: Media?, newDuration: Long) {
            duration = newDuration
        }
    }

    val playerListener = object : MediaPlayerEventAdapter() {
        override fun playing(mediaPlayer: MediaPlayer?) { isPlaying = true }
        override fun paused(mediaPlayer: MediaPlayer?) { isPlaying = false }
        override fun timeChanged(mediaPlayer: MediaPlayer?, newTime: Long) { position = newTime }
        override fun stopped(mediaPlayer: MediaPlayer?) { mediaPlayer?.media()?.play(url) }
        override fun error(mediaPlayer: MediaPlayer?) { mediaPlayer?.controls()?.stop() }
    }

    inline fun withPlayer(block: (EmbeddedMediaPlayer) -> Unit) {
        component.value?.mediaPlayer()?.let(block)
    }

    fun play() = withPlayer { player ->
        if (!player.status().isPlaying) player.controls().play()
    }

    fun pause() = withPlayer { player ->
        if (player.status().isPlaying) player.controls().pause()
    }

    fun seekTo(value: Long) = withPlayer { player ->
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
    val state by rememberState { VideoPlayerState(url) }

    OffScreenEffect { isForeground ->
        if (isForeground) state.play()
        else state.pause()
    }

    Box(modifier = modifier) {
        CustomUI(
            view = state.component,
            modifier = Modifier.fillMaxSize().zIndex(1f),
            factory = {
                val component = CallbackMediaPlayerComponent()
                SwingUtilities.invokeLater {
                    component.mediaPlayer().apply {
                        events().apply {
                            addMediaEventListener(state.eventListener)
                            addMediaPlayerEventListener(state.playerListener)
                        }
                        media().play(url)
                    }
                }
                component
            },
            release = { component, onRelease ->
                component.mediaPlayer().events().apply {
                    removeMediaEventListener(state.eventListener)
                    removeMediaPlayerEventListener(state.playerListener)
                }
                component.release()
                onRelease()
            }
        )

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