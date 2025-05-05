package love.yinlin.ui.screen.music.loader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.ui.component.screen.SubScreen

@Stable
class ScreenKugouMusic(model: AppModel, private val args: Args) : SubScreen<ScreenKugouMusic.Args>(model) {
    @Stable
    @Serializable
    data class Args(val shareUrl: String?)

    override val title: String = "酷狗音乐"

    @Composable
    override fun SubContent(device: Device) {

    }
}