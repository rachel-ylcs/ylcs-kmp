package love.yinlin.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.app
import love.yinlin.compose.Device
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.extension.lazyProvider
import love.yinlin.extension.moveItem
import love.yinlin.media.lyrics.FloatingLyrics
import love.yinlin.startup.StartupMusicPlayer
import sh.calvin.reorderable.ReorderableColumn

@Composable
expect fun ScreenLyricsSettings.PlatformContent()

@Stable
class ScreenLyricsSettings : Screen() {
    internal val mp by lazyProvider { app.startup<StartupMusicPlayer>() }

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

    override val title: String = "歌词设置"

    @Composable
    override fun Content() {
        Column(
            modifier = Modifier
                .padding(LocalImmersivePadding.current)
                .condition(LocalDevice.current.type == Device.Type.PORTRAIT, ifTrue = { fillMaxWidth() }, ifFalse = { width(Theme.size.cell1 * 1.5f) })
                .fillMaxHeight()
                .padding(Theme.padding.eValue9)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
        ) {
            SimpleEllipsisText("歌词引擎优先级", color = Theme.color.primary, style = Theme.typography.v6.bold)

            ReorderableColumn(
                list = app.config.lyricsEngineOrder,
                onSettle = { from, to ->
                    app.config.lyricsEngineOrder = app.config.lyricsEngineOrder.toMutableList().also { it.moveItem(from, to) }
                },
                modifier = Modifier.fillMaxWidth()
            ) { _, item, _ ->
                key(item.key) {
                    ReorderableItem {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { }.padding(Theme.padding.value),
                            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon = item.icon)
                            SimpleEllipsisText(item.title, modifier = Modifier.weight(1f))
                            Icon(icon = Icons.DragHandle, modifier = Modifier.draggableHandle())
                        }
                    }
                }
            }

            SimpleEllipsisText("悬浮歌词", color = Theme.color.primary, style = Theme.typography.v6.bold)
            PlatformContent()
        }
    }
}