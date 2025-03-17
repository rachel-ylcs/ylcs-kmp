package love.yinlin.ui.screen.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.ui.screen.Screen
import love.yinlin.ui.component.extra.WebPage
import love.yinlin.ui.component.extra.WebPageSettings
import love.yinlin.ui.component.extra.WebPageState
import love.yinlin.ui.component.screen.SubScreen

@Stable
@Serializable
data class ScreenWebpage(val url: String) : Screen<ScreenWebpage.Model> {
	inner class Model(model: AppModel) : Screen.Model(model) {
		val state = WebPageState(WebPageSettings(), url)
	}

	override fun model(model: AppModel): Model = Model(model)

	@Composable
	override fun content(model: Model) {
		SubScreen(
			modifier = Modifier.fillMaxSize(),
			title = model.state.title,
			onBack = {
				if (model.state.canGoBack) model.state.goBack()
				else model.pop()
			}
		) {
			WebPage(
				state = model.state,
				modifier = Modifier.fillMaxSize()
			)
		}
	}
}