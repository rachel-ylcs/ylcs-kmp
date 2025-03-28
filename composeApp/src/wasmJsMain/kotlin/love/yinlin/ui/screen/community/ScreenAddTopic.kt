package love.yinlin.ui.screen.community

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.ui.component.common.UnsupportedScreen
import love.yinlin.ui.screen.Screen

@Stable
@Serializable
actual data object ScreenAddTopic : Screen<ScreenAddTopic.Model> {
    actual class Model(model: AppModel) : Screen.Model(model)

    actual override fun model(model: AppModel): Model = Model(model)

    @Composable
    actual override fun content(model: Model) {
        UnsupportedScreen(model)
    }
}