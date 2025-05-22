package love.yinlin.ui.screen.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.ScreenPart
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.platform.OS
import love.yinlin.platform.Platform
import love.yinlin.ui.component.platform.WebPage
import love.yinlin.ui.component.platform.WebPageSettings
import love.yinlin.ui.component.platform.WebPageState
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.Screen

@Stable
class ScreenWebpage(model: AppModel, args: Args) : SubScreen<ScreenWebpage.Args>(model) {
	@Stable
	@Serializable
	data class Args(val url: String)

	companion object {
		inline fun gotoWebPage(arg: String, onNavigate: (Args) -> Unit) {
			OS.ifPlatform(
				Platform.WebWasm, *Platform.Desktop,
				ifTrue = {
					OS.Net.openUrl(arg)
				},
				ifFalse = {
					onNavigate(Args(arg))
				}
			)
		}
		fun ScreenPart.gotoWebPage(arg: String) = gotoWebPage(arg) { navigate(it) }
		fun Screen<*>.gotoWebPage(arg: String) = gotoWebPage(arg) { navigate(it) }
	}

	private val state = WebPageState(WebPageSettings(), args.url)

	override val title: String by derivedStateOf { state.title }

	override fun onBack() {
		if (state.canGoBack) state.goBack()
		else pop()
	}

	@Composable
	override fun SubContent(device: Device) {
		WebPage(
			state = state,
			modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
		)
	}
}