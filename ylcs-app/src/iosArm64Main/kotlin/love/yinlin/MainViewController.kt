package love.yinlin

import androidx.compose.ui.window.ComposeUIViewController
import love.yinlin.platform.ActualAppContext
import love.yinlin.platform.app

fun MainViewController() {
	val context = ActualAppContext()
	app = context
	context.initialize()
	ComposeUIViewController {
		AppWrapper {
			App()
		}
	}
}