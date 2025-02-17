package love.yinlin

import androidx.compose.ui.window.ComposeUIViewController
import love.yinlin.platform.AppContext
import love.yinlin.platform.appContext

fun MainViewController() {
	appContext = AppContext().initialize()
	ComposeUIViewController {
		AppWrapper {
			App()
		}
	}
}