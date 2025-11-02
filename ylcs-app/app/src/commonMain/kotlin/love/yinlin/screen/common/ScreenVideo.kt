package love.yinlin.screen.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import love.yinlin.compose.Device
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.platform.VideoPlayer

@Stable
class ScreenVideo(manager: ScreenManager, val url: String) : Screen(manager) {
    override val title: String? = null

    @Composable
    override fun Content(device: Device) {
        VideoPlayer(
            url = url,
            modifier = Modifier.fillMaxSize(),
            onBack = { onBack() }
        )
    }
}