package love.yinlin.ui.screen.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.common.Colors
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.extension.rememberState
import love.yinlin.platform.ActualFloatingLyrics
import love.yinlin.platform.app
import love.yinlin.ui.component.input.BeautifulSlider
import love.yinlin.ui.component.input.DockedColorPicker
import love.yinlin.ui.component.input.Switch
import love.yinlin.ui.component.layout.SplitLayout

@Composable
actual fun ScreenFloatingLyrics.ActualContent(device: Device) {
    var iosConfig by rememberState { app.config.floatingLyricsIOSConfig }

    Column(modifier = Modifier
        .padding(LocalImmersivePadding.current)
        .fillMaxSize()
        .padding(ThemeValue.Padding.EqualExtraValue)
        .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
    ) {
        RowLayout("悬浮歌词模式") {
            Switch(
                checked = app.config.enabledFloatingLyrics,
                onCheckedChange = { value ->
                    (app.musicFactory.floatingLyrics as? ActualFloatingLyrics)?.let { floatingLyrics ->
                        if (value) {
                            if (floatingLyrics.canAttached) {
                                floatingLyrics.attach()
                            } else {
                                // 模拟器和旧机型不支持
                                slot.tip.error("该设备不支持悬浮歌词")
                            }
                        } else {
                            floatingLyrics.detach()
                        }
                    }
                }
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "这是一条测试歌词~",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = MaterialTheme.typography.labelLarge.fontSize * iosConfig.textSize
                    ),
                    color = Colors.from(iosConfig.textColor),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.background(color = Colors.from(iosConfig.backgroundColor)).padding(ThemeValue.Padding.Value)
                )
            }
        }

        RowLayout("字体大小") {
            BeautifulSlider(
                value = iosConfig.textSizeProgress,
                onValueChange = { iosConfig = iosConfig.copyTextSize(it) },
                onValueChangeFinished = { app.config.floatingLyricsIOSConfig = iosConfig },
                modifier = Modifier.fillMaxWidth().padding(horizontal = ThemeValue.Padding.HorizontalExtraSpace)
            )
        }

        SplitLayout(
            modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.ExtraValue),
            horizontalArrangement = ThemeValue.Padding.HorizontalExtraSpace * 2,
            left = {
                ColumnLayout("字体颜色") {
                    DockedColorPicker(
                        initialColor = Colors.from(app.config.floatingLyricsIOSConfig.textColor),
                        onColorChanged = { iosConfig = iosConfig.copy(textColor = it.value) },
                        onColorChangeFinished = { app.config.floatingLyricsIOSConfig = iosConfig },
                        modifier = Modifier.widthIn(max = ThemeValue.Size.CellWidth).fillMaxWidth()
                    )
                }
            },
            right = {
                ColumnLayout("背景颜色") {
                    DockedColorPicker(
                        initialColor = Colors.from(app.config.floatingLyricsIOSConfig.backgroundColor),
                        onColorChanged = { iosConfig = iosConfig.copy(backgroundColor = it.value) },
                        onColorChangeFinished = { app.config.floatingLyricsIOSConfig = iosConfig },
                        modifier = Modifier.widthIn(max = ThemeValue.Size.CellWidth).fillMaxWidth()
                    )
                }
            }
        )
    }
}