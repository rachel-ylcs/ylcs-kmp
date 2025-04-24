package love.yinlin.ui.component.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import love.yinlin.common.Colors
import love.yinlin.extension.rememberState
import love.yinlin.ui.component.CustomUI
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import javax.swing.SwingUtilities

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier
) {
    val mediaPlayer: MutableState<CallbackMediaPlayerComponent?> = rememberState { null }

    Box(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize().background(Colors.Black)) {
            CustomUI(
                view = mediaPlayer,
                modifier = Modifier.fillMaxSize(),
                factory = {
                    val component = CallbackMediaPlayerComponent()
                    SwingUtilities.invokeLater {
                        component.mediaPlayer().media().play(url)
                    }
                    component
                },
                release = { player, onRelease ->
                    player.release()
                    onRelease()
                }
            )
        }
    }
}