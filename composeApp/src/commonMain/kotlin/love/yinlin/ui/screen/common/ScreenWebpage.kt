package love.yinlin.ui.screen.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import love.yinlin.AppModel
import love.yinlin.ui.component.extra.WebPage
import love.yinlin.ui.component.extra.WebPageSettings
import love.yinlin.ui.component.extra.WebPageState
import love.yinlin.ui.component.screen.SubScreen

class WebPageModel(url: String) : ViewModel() {
	val state = WebPageState(WebPageSettings(), url)
}

@Composable
fun ScreenWebPage(model: AppModel, url: String) {
	val screenModel = viewModel { WebPageModel(url) }

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