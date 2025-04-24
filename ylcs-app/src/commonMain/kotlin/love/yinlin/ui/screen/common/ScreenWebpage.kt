package love.yinlin.ui.screen.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.ui.screen.Screen
import love.yinlin.ui.component.platform.WebPage
import love.yinlin.ui.component.platform.WebPageSettings
import love.yinlin.ui.component.platform.WebPageState
import love.yinlin.ui.component.screen.SubScreen

@Stable
class ScreenWebpage(model: AppModel, args: Args) : Screen<ScreenWebpage.Args>(model) {
	@Stable
	@Serializable
	data class Args(val url: String) : Screen.Args

	private val state = WebPageState(WebPageSettings(), args.url)

	@Composable
	override fun Content() {
		SubScreen(
			modifier = Modifier.fillMaxSize(),
			title = state.title,
			onBack = {
				if (state.canGoBack) state.goBack()
				else pop()
			},
			slot = slot
		) {
			WebPage(
				state = state,
				modifier = Modifier.fillMaxSize()
			)
		}
	}
}