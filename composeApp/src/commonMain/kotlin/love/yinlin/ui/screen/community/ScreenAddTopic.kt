package love.yinlin.ui.screen.community

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.ui.screen.Screen

@Stable
@Serializable
expect object ScreenAddTopic : Screen<ScreenAddTopic.Model> {
    class Model : Screen.Model

    override fun model(model: AppModel): Model

    @Composable
    override fun content(model: Model)
}