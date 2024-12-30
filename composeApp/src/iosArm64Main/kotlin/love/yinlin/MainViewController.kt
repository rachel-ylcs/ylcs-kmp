package love.yinlin

import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.viewmodel.compose.viewModel
import love.yinlin.model.AppModel
import love.yinlin.platform.IOSContext
import love.yinlin.platform.KV

fun MainViewController() {
	val context = IOSContext()
	val kv = KV()
	ComposeUIViewController {
		AppWrapper(context) {
			App(viewModel { AppModel(kv) })
		}
	}
}