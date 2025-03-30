package love.yinlin.ui.screen.world

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.ui.screen.Screen
import love.yinlin.ui.component.common.UnsupportedScreen

@Stable
@Serializable
actual data object ScreenAddActivity : Screen<ScreenAddActivity.Model> {
	actual class Model(model: AppModel) : Screen.Model(model)

	actual override fun model(model: AppModel): Model = Model(model)

	@Composable
	actual override fun content(model: Model) {
		UnsupportedScreen(model)
	}
}