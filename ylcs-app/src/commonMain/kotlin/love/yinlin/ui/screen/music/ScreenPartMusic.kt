package love.yinlin.ui.screen.music

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import love.yinlin.AppModel
import love.yinlin.ScreenPart
import love.yinlin.platform.app
import love.yinlin.ui.component.input.RachelButton

class ScreenPartMusic(model: AppModel) : ScreenPart(model) {
	@Composable
	override fun content() {
		val factory = app.musicFactory
		val scope = rememberCoroutineScope()

		Column {
			Text("${factory.isInit}")
			Text("${factory.musicLibrary}")
			Text("${factory.playlistLibrary}")
			RachelButton("start") {
				scope.launch {
					factory.start(factory.playlistLibrary["测试"]!!)
				}
			}
			RachelButton("Play") {
				scope.launch {
					factory.play()
				}
			}
			RachelButton("Pause") {
				scope.launch {
					factory.pause()
				}
			}
		}
	}
}