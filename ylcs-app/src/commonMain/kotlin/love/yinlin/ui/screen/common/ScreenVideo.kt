package love.yinlin.ui.screen.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.ui.component.platform.VideoPlayer
import love.yinlin.ui.component.screen.FloatingDialogProgress
import love.yinlin.ui.component.screen.SubScreen

@Stable
class ScreenVideo(model: AppModel, val args: Args) : SubScreen<ScreenVideo.Args>(model) {
    @Stable
    @Serializable
    data class Args(val url: String)

    private val downloadDialog = FloatingDialogProgress()

    override val title: String? = null

    @Composable
    override fun SubContent(device: Device) {
        VideoPlayer(
            url = args.url,
            modifier = Modifier.fillMaxSize(),
            onBack = { onBack() }
        )
    }

    @Composable
    override fun Floating() {
        downloadDialog.Land()
    }
}