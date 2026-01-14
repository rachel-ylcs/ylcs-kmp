package love.yinlin.screen.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.app
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.collection.toStableList
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.CustomTheme
import love.yinlin.compose.ui.input.SortingBox
import love.yinlin.extension.moveItem
import love.yinlin.platform.lyrics.FloatingLyrics

@Composable
expect fun ScreenLyricsSettings.PlatformContent()

@Stable
class ScreenLyricsSettings(manager: ScreenManager) : Screen(manager) {
    override val title: String = "歌词设置"

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
    override fun Content(device: Device) {
        Column(
            modifier = Modifier
                .padding(LocalImmersivePadding.current)
                .fillMaxSize()
                .padding(CustomTheme.padding.equalExtraValue)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
        ) {
            ColumnLayout("歌词引擎优先级") {
                SortingBox(
                    items = remember(app.config.lyricsEngineOrder) { app.config.lyricsEngineOrder.toStableList() },
                    onMove = { from, to -> app.config.lyricsEngineOrder = app.config.lyricsEngineOrder.toMutableList().also { it.moveItem(from, to) } },
                    title = { it.title },
                    icon = { it.icon },
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue)
                )
            }
            ColumnLayout("悬浮歌词设置") {
                PlatformContent()
            }
        }
    }

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