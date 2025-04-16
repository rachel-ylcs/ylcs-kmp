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
@Serializable
data object ScreenPlaylistLibrary : Screen<ScreenPlaylistLibrary.Model> {
    class Model(model: AppModel) : Screen.Model(model) {

    }

    override fun model(model: AppModel): Model = Model(model).apply {

    }

    @Composable
    override fun content(model: Model) {
        SubScreen(
            modifier = Modifier.fillMaxSize(),
            title = "歌单",
            onBack = { model.pop() },
            slot = model.slot
        ) {

        }
    }
}