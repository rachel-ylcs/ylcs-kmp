package love.yinlin.ui.component.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import love.yinlin.extension.OffScreenEffect
import love.yinlin.extension.clickableNoRipple
import love.yinlin.extension.rememberState
import love.yinlin.ui.component.CustomUI
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import javax.swing.SwingUtilities

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier
) {
    val controller: MutableState<CallbackMediaPlayerComponent?> = rememberState { null }

    OffScreenEffect { isForeground ->
        controller.value?.mediaPlayer()?.let {
            if (isForeground) {
                if (!it.status().isPlaying) it.controls().play()
            }
            else {
                if (it.status().isPlaying) it.controls().pause()
            }
        }
    }

    Box(modifier = modifier) {
        CustomUI(
            view = controller,
            modifier = Modifier.fillMaxSize(),
            factory = {
                val component = CallbackMediaPlayerComponent()
                SwingUtilities.invokeLater {
                    component.mediaPlayer().apply {
                        events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
                            override fun stopped(player: MediaPlayer?) {
                                player?.media()?.play(url)
                            }
                        })
                        media().play(url)
                    }
                }
                component
            },
            release = { player, onRelease ->
                player.release()
                onRelease()
            }
        )
        Box(modifier = Modifier.fillMaxSize().clickableNoRipple {
            controller.value?.mediaPlayer()?.let {
                if (!it.status().isPlaying) it.controls().play()
                else it.controls().pause()
            }
        }.zIndex(2f))
    }
}