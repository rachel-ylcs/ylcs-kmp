package love.yinlin.ui.screen.music

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.Screen

@Stable
class ScreenImportMusic(model: AppModel, args: Args) : Screen<ScreenImportMusic.Args>(model) {
    @Stable
    @Serializable
    data class Args(val uri: String?) : Screen.Args

    @Composable
    override fun content() {
        SubScreen(
            modifier = Modifier.fillMaxSize(),
            title = "导入歌曲",
            onBack = { pop() },
            slot = slot
        ) {

        }
    }
}