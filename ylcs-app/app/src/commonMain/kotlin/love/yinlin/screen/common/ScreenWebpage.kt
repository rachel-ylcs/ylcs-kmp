package love.yinlin.screen.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.platform.OS
import love.yinlin.platform.Platform
import love.yinlin.ui.component.platform.WebPage
import love.yinlin.ui.component.platform.WebPageSettings
import love.yinlin.ui.component.platform.WebPageState

@Stable
class ScreenWebpage(manager: ScreenManager, args: Args) : Screen<ScreenWebpage.Args>(manager) {
	@Stable
	@Serializable
	data class Args(val url: String)

	companion object {
		inline fun gotoWebPage(arg: String, onNavigate: (Args) -> Unit) {
			Platform.use(
				*Platform.Desktop,
				ifTrue = { OS.Net.openUrl(arg) },
				ifFalse = { onNavigate(Args(arg)) }
			)
		}
	}

	private val state = WebPageState(WebPageSettings(), args.url)

	override val title: String by derivedStateOf { state.title }

	override fun onBack() {
		if (state.canGoBack) state.goBack()
		else pop()
	}

	@Composable
	override fun Content(device: Device) {
		WebPage(
			state = state,
			modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
		)
	}
}