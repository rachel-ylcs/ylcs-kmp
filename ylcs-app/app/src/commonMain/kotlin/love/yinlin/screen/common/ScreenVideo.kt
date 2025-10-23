package love.yinlin.screen.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable
import love.yinlin.compose.Device
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.ui.component.platform.VideoPlayer

@Stable
class ScreenVideo(manager: ScreenManager, val args: Args) : Screen<ScreenVideo.Args>(manager) {
    @Stable
    @Serializable
    data class Args(val url: String)

    override val title: String? = null

    @Composable
    override fun Content(device: Device) {
        VideoPlayer(
            url = args.url,
            modifier = Modifier.fillMaxSize(),
            onBack = { onBack() }
        )
    }
}