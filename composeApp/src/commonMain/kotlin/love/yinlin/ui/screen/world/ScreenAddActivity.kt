package love.yinlin.ui.screen.world

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.ui.Screen

@Stable
@Serializable
expect object ScreenAddActivity : Screen<ScreenAddActivity.Model> {
	class Model : Screen.Model

	override fun model(model: AppModel): Model

	@Composable
	override fun content(model: Model)
}