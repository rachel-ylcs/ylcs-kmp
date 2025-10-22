package love.yinlin.ui.screen.music

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import love.yinlin.compose.*
import love.yinlin.platform.ActualFloatingLyrics
import love.yinlin.platform.Coroutines
import love.yinlin.platform.Platform
import love.yinlin.platform.app
import love.yinlin.ui.component.input.DockedColorPicker
import love.yinlin.ui.component.input.ProgressSlider
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.layout.SplitLayout

// ignoresMouseEvents on macOS is buggy, see https://stackoverflow.com/questions/29441015
private fun macosClickFixup(floatingLyrics: ActualFloatingLyrics) =
    Platform.use(Platform.MacOS) {
        if (floatingLyrics.isAttached) {
            floatingLyrics.isAttached = false
            Coroutines.startMain {
                delay(100)
                floatingLyrics.isAttached = true
            }
        }
    }

@Composable
actual fun ScreenFloatingLyrics.ActualContent(device: Device) {
    var desktopConfig by rememberRefState { app.config.floatingLyricsDesktopConfig }

    Column(modifier = Modifier
        .padding(LocalImmersivePadding.current)
        .fillMaxSize()
        .padding(CustomTheme.padding.equalExtraValue)
        .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
    ) {
        (app.musicFactory.floatingLyrics as? ActualFloatingLyrics)?.let { floatingLyrics ->
            DisposableEffect(Unit) {
                desktopConfig = app.config.floatingLyricsDesktopConfig
                floatingLyrics.canMove = true
                macosClickFixup(floatingLyrics)
                onDispose {
                    floatingLyrics.canMove = false
                    macosClickFixup(floatingLyrics)
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
                    value = desktopConfig.textSizeProgress,
                    onValueChange = { desktopConfig = desktopConfig.copyTextSize(it) },
                    onValueChangeFinished = { app.config.floatingLyricsDesktopConfig = desktopConfig },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.horizontalExtraSpace)
                )
            }

            SplitLayout(
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.extraValue),
                horizontalArrangement = CustomTheme.padding.horizontalExtraSpace * 2,
                left = {
                    ColumnLayout("字体颜色") {
                        DockedColorPicker(
                            initialColor = Colors(app.config.floatingLyricsDesktopConfig.textColor),
                            onColorChanged = { desktopConfig = desktopConfig.copy(textColor = it.value) },
                            onColorChangeFinished = { app.config.floatingLyricsDesktopConfig = desktopConfig },
                            modifier = Modifier.widthIn(max = CustomTheme.size.cellWidth).fillMaxWidth()
                        )
                    }
                },
                right = {
                    ColumnLayout("背景颜色") {
                        DockedColorPicker(
                            initialColor = Colors(app.config.floatingLyricsDesktopConfig.backgroundColor),
                            onColorChanged = { desktopConfig = desktopConfig.copy(backgroundColor = it.value) },
                            onColorChangeFinished = { app.config.floatingLyricsDesktopConfig = desktopConfig },
                            modifier = Modifier.widthIn(max = CustomTheme.size.cellWidth).fillMaxWidth()
                        )
                    }
                }
            )
        }
    }
}