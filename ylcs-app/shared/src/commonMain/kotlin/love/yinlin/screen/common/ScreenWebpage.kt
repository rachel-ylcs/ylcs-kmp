package love.yinlin.screen.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import love.yinlin.app
import love.yinlin.uri.Uri
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.WebView
import love.yinlin.compose.ui.WebViewState
import love.yinlin.platform.Platform

@Stable
class ScreenWebpage(manager: ScreenManager, url: String) : Screen(manager) {
	companion object {
		inline fun gotoWebPage(arg: String, onNavigate: (String) -> Unit) {
			Platform.use(
				*Platform.Desktop,
				ifTrue = { Uri.parse(arg)?.let { app.os.net.openUri(it) } },
				ifFalse = { onNavigate(arg) }
			)
		}
	}

	private val state = WebViewState(url)

	override val title: String by derivedStateOf { state.title }

	override fun onBack() {
		if (state.canGoBack) state.goBack()
		else pop()
	}

	@Composable
	override fun Content(device: Device) {
		WebView(
			state = state,
			modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
		)
	}
}