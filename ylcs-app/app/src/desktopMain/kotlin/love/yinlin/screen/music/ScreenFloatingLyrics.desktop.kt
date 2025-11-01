package love.yinlin.screen.music

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import love.yinlin.app
import love.yinlin.compose.*
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.input.DockedColorPicker
import love.yinlin.compose.ui.input.ProgressSlider
import love.yinlin.compose.ui.layout.SplitLayout
import love.yinlin.fixup.FixupMacOSMouseClick

@Composable
actual fun ScreenFloatingLyrics.platformContent(device: Device) {
    Column(modifier = Modifier
        .padding(LocalImmersivePadding.current)
        .fillMaxSize()
        .padding(CustomTheme.padding.equalExtraValue)
        .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
    ) {
        val floatingLyrics = app.mp.floatingLyrics
        DisposableEffect(Unit) {
            config = app.config.lyricsEngineConfig
            floatingLyrics.canMove = true
            FixupMacOSMouseClick.setupDelay(floatingLyrics.isAttached) { floatingLyrics.isAttached = it }
            onDispose {
                floatingLyrics.canMove = false
                FixupMacOSMouseClick.setupDelay(floatingLyrics.isAttached) { floatingLyrics.isAttached = it }
            }
        }

        RowLayout("悬浮歌词模式") {
            Switch(
                checked = app.config.enabledFloatingLyrics,
                onCheckedChange = {
                    floatingLyrics.isAttached = it
                    app.config.enabledFloatingLyrics = it
                }
            )
        }

        RowLayout("字体大小") {
            ProgressSlider(
                value = config.textSizeProgress,
                onValueChange = { config = config.copyTextSize(it) },
                onValueChangeFinished = { app.config.lyricsEngineConfig = config },
                modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.horizontalExtraSpace)
            )
        }

        SplitLayout(
            modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.extraValue),
            horizontalArrangement = CustomTheme.padding.horizontalExtraSpace * 2,
            left = {
                ColumnLayout("字体颜色") {
                    DockedColorPicker(
                        initialColor = Colors(app.config.lyricsEngineConfig.textColor),
                        onColorChanged = { config = config.copy(textColor = it.value) },
                        onColorChangeFinished = { app.config.lyricsEngineConfig = config },
                        modifier = Modifier.widthIn(max = CustomTheme.size.cellWidth).fillMaxWidth()
                    )
                }
            },
            right = {
                ColumnLayout("背景颜色") {
                    DockedColorPicker(
                        initialColor = Colors(app.config.lyricsEngineConfig.backgroundColor),
                        onColorChanged = { config = config.copy(backgroundColor = it.value) },
                        onColorChangeFinished = { app.config.lyricsEngineConfig = config },
                        modifier = Modifier.widthIn(max = CustomTheme.size.cellWidth).fillMaxWidth()
                    )
                }
            }
        )
    }
}