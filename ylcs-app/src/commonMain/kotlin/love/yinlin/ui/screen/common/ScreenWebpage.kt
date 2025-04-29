package love.yinlin.ui.screen.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.common.Orientation
import love.yinlin.ui.component.platform.WebPage
import love.yinlin.ui.component.platform.WebPageSettings
import love.yinlin.ui.component.platform.WebPageState
import love.yinlin.ui.component.screen.SubScreen

@Stable
class ScreenWebpage(model: AppModel, args: Args) : SubScreen<ScreenWebpage.Args>(model) {
	@Stable
	@Serializable
	data class Args(val url: String)

	private val state = WebPageState(WebPageSettings(), args.url)

	override val title: String by derivedStateOf { state.title }

	override fun onBack() {
		if (state.canGoBack) state.goBack()
		else pop()
	}

	@Composable
	override fun SubContent(orientation: Orientation) {
		WebPage(
			state = state,
			modifier = Modifier.fillMaxSize()
		)
	}
}