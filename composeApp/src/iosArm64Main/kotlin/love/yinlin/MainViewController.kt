package love.yinlin

import androidx.compose.ui.window.ComposeUIViewController
import love.yinlin.platform.AppContext
import love.yinlin.platform.app

fun MainViewController() {
	app = AppContext().initialize()
	ComposeUIViewController {
		AppWrapper {
			App()
		}
	}
}