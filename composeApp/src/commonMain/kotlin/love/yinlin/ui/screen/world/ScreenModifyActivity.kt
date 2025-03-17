package love.yinlin.ui.screen.world

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.ui.screen.Screen

@Stable
@Serializable
expect class ScreenModifyActivity : Screen<ScreenModifyActivity.Model> {
	class Model : Screen.Model

	constructor(aid: Int)

	override fun model(model: AppModel): Model

	@Composable
	override fun content(model: Model)
}