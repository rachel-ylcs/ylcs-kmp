package love.yinlin.ui.screen.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.AppModel
import love.yinlin.common.ScreenModel
import love.yinlin.common.screen
import love.yinlin.ui.component.extra.WebPage
import love.yinlin.ui.component.extra.WebPageSettings
import love.yinlin.ui.component.extra.WebPageState
import love.yinlin.ui.component.screen.SubScreen

private class WebPageModel(url: String) : ScreenModel() {
	val state = WebPageState(WebPageSettings(), url)
}

@Composable
fun ScreenWebPage(model: AppModel, url: String) {
	val screenModel = screen { WebPageModel(url) }

	SubScreen(
		modifier = Modifier.fillMaxSize(),
		title = screenModel.state.title,
		onBack = {
			if (screenModel.state.canGoBack) screenModel.state.goBack()
			else model.pop()
		}
	) {
		WebPage(
			state = screenModel.state,
			modifier = Modifier.fillMaxSize()
		)
	}
}