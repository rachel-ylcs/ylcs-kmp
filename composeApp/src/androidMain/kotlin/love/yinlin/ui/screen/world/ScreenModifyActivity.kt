package love.yinlin.ui.screen.world

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.ui.Screen

@Stable
@Serializable
actual data class ScreenModifyActivity(val aid: Int) : Screen<ScreenModifyActivity.Model> {
	actual class Model(model: AppModel) : Screen.Model(model)

	actual override fun model(model: AppModel): Model = Model(model)

	@Composable
	actual override fun content(model: Model) {

	}
}