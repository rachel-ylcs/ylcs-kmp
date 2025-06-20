@file:JvmName("DesktopScreenFloatingLyrics")
package love.yinlin.ui.screen.music

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import love.yinlin.common.Colors
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.extension.rememberState
import love.yinlin.platform.ActualFloatingLyrics
import love.yinlin.platform.Coroutines
import love.yinlin.platform.OS
import love.yinlin.platform.Platform
import love.yinlin.platform.app
import love.yinlin.ui.component.input.ProgressSlider
import love.yinlin.ui.component.input.DockedColorPicker
import love.yinlin.ui.component.input.Switch
import love.yinlin.ui.component.layout.SplitLayout

// ignoresMouseEvents on macOS is buggy, see https://stackoverflow.com/questions/29441015
private fun macosClickFixup(floatingLyrics: ActualFloatingLyrics) =
    OS.ifPlatform(Platform.MacOS, block = {
        if (floatingLyrics.isAttached) {
            floatingLyrics.isAttached = false
            Coroutines.startMain {
                delay(100)
                floatingLyrics.isAttached = true
            }
        }
    })

@Composable
actual fun ScreenFloatingLyrics.ActualContent(device: Device) {
    var desktopConfig by rememberState { app.config.floatingLyricsDesktopConfig }

    Column(modifier = Modifier
        .padding(LocalImmersivePadding.current)
        .fillMaxSize()
        .padding(ThemeValue.Padding.EqualExtraValue)
        .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ThemeValue.Padding.HorizontalExtraSpace)
                )
            }

            SplitLayout(
                modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.ExtraValue),
                horizontalArrangement = ThemeValue.Padding.HorizontalExtraSpace * 2,
                left = {
                    ColumnLayout("字体颜色") {
                        DockedColorPicker(
                            initialColor = Colors.from(app.config.floatingLyricsDesktopConfig.textColor),
                            onColorChanged = { desktopConfig = desktopConfig.copy(textColor = it.value) },
                            onColorChangeFinished = { app.config.floatingLyricsDesktopConfig = desktopConfig },
                            modifier = Modifier.widthIn(max = ThemeValue.Size.CellWidth).fillMaxWidth()
                        )
                    }
                },
                right = {
                    ColumnLayout("背景颜色") {
                        DockedColorPicker(
                            initialColor = Colors.from(app.config.floatingLyricsDesktopConfig.backgroundColor),
                            onColorChanged = { desktopConfig = desktopConfig.copy(backgroundColor = it.value) },
                            onColorChangeFinished = { app.config.floatingLyricsDesktopConfig = desktopConfig },
                            modifier = Modifier.widthIn(max = ThemeValue.Size.CellWidth).fillMaxWidth()
                        )
                    }
                }
            )
        }
    }
}