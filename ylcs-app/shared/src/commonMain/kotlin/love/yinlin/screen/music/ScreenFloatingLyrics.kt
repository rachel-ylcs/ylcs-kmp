package love.yinlin.screen.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.app
import love.yinlin.compose.*
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.platform.lyrics.FloatingLyrics

@Composable
expect fun ScreenFloatingLyrics.PlatformContent(device: Device)

@Stable
class ScreenFloatingLyrics(manager: ScreenManager) : Screen(manager) {
    override val title: String = "悬浮歌词"

    internal var config by mutableRefStateOf(app.config.lyricsEngineConfig)

    internal fun FloatingLyrics.check() {
        if (app.config.enabledFloatingLyrics) {
            // 如果当前处于启用悬浮歌词状态, 但悬浮歌词没有附加上去则附加
            if (!isAttached) attach()
        }
        else {
            // 如果当前处于禁用悬浮歌词状态, 但悬浮歌词附加上去了则脱离
            if (isAttached) detach()
        }
    }

    @Composable
    override fun Content(device: Device) = PlatformContent(device)

    @Composable
    internal fun RowLayout(
        title: String,
        content: @Composable () -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace * 2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                content()
            }
        }
    }

    @Composable
    internal fun ColumnLayout(
        title: String,
        content: @Composable () -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace * 2)
        ) {
            Text(text = title)
            content()
        }
    }
}