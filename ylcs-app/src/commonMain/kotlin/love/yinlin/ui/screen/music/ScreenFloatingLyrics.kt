package love.yinlin.ui.screen.music

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.platform.FloatingLyrics
import love.yinlin.platform.app
import love.yinlin.ui.component.screen.CommonSubScreen

@Composable
expect fun ScreenFloatingLyrics.ActualContent(device: Device)

@Stable
class ScreenFloatingLyrics(model: AppModel) : CommonSubScreen(model) {
    var enabled: Boolean by mutableStateOf(app.musicFactory.floatingLyrics?.isAttached ?: false)
    var androidConfig: FloatingLyrics.AndroidConfig by mutableStateOf(app.config.floatingLyricsAndroidConfig)
    var iOSConfig: FloatingLyrics.IOSConfig by mutableStateOf(app.config.floatingLyricsIOSConfig)
    var desktopConfig: FloatingLyrics.DesktopConfig by mutableStateOf(app.config.floatingLyricsDesktopConfig)

    override val title: String = "悬浮歌词"

    @Composable
    override fun SubContent(device: Device) {
        ActualContent(device)
    }
}