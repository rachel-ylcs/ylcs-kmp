package love.yinlin.screen.music

import androidx.compose.runtime.*
import love.yinlin.app
import love.yinlin.compose.Device
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager

@Stable
class ScreenMusicDetails(manager: ScreenManager, id: String) : Screen(manager) {
    private val musicInfo by derivedStateOf { app.mp.library[id] }

    override val title: String = musicInfo?.name ?: "未知歌曲"

    @Composable
    override fun Content(device: Device) {

    }
}