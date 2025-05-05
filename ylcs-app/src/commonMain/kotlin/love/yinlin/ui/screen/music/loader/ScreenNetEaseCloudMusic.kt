package love.yinlin.ui.screen.music.loader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.ui.component.screen.SubScreen

@Stable
class ScreenNetEaseCloudMusic(model: AppModel, private val args: Args) : SubScreen<ScreenNetEaseCloudMusic.Args>(model) {
    @Stable
    @Serializable
    data class Args(val shareUrl: String?)

    override val title: String = "网易云音乐"

    @Composable
    override fun SubContent(device: Device) {

    }
}