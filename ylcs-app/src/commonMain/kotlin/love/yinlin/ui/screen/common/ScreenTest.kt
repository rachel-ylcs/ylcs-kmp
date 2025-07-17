package love.yinlin.ui.screen.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.platform.MusicPlayer
import love.yinlin.platform.app
import love.yinlin.ui.component.input.LoadingRachelButton
import love.yinlin.ui.component.screen.CommonSubScreen
import love.yinlin.ui.screen.music.audioPath

@Stable
class ScreenTest(model: AppModel) : CommonSubScreen(model) {
    override val title: String = "测试页"

    private val player = MusicPlayer()

    override suspend fun initialize() {
        player.init()
    }

    override fun finalize() {
        player.release()
    }

    @Composable
    override fun SubContent(device: Device) {
        Column(modifier = Modifier.fillMaxSize()) {
            LoadingRachelButton("载入") {
                app.musicFactory.musicLibrary.values.firstOrNull()?.audioPath?.let { player.load(it) }
            }
            LoadingRachelButton("播放/暂停") {
                if (player.isPlaying) player.pause()
                else player.play()
            }
            LoadingRachelButton("停止") {
                player.stop()
            }
            LoadingRachelButton("调进度") {
                player.position = player.duration - 5000L
            }
            Text(text = "isInit = ${player.isInit}")
            Text(text = "isPlaying = ${player.isPlaying}")
            Text(text = "position = ${player.position}")
            Text(text = "duration = ${player.duration}")
        }
    }
}