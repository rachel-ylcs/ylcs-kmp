package love.yinlin

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() {
	appContext = IOSContext().initialize()
	ComposeUIViewController {
		AppWrapper {
			App()
		}
	}
}