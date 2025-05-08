package love.yinlin.ui.screen.music

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.ui.component.screen.CommonSubScreen

@Stable
class ScreenFloatingLyrics(model: AppModel) : CommonSubScreen(model) {
    override val title: String = "悬浮歌词"

    @Composable
    override fun SubContent(device: Device) {

    }
}