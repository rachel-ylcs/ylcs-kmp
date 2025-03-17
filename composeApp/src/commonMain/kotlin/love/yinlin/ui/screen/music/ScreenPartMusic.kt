package love.yinlin.ui.screen.music

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import love.yinlin.AppModel
import love.yinlin.ScreenPart

class ScreenPartMusic(model: AppModel) : ScreenPart(model) {
	@Composable
	override fun content() {
		Text("Screen Music")
	}
}