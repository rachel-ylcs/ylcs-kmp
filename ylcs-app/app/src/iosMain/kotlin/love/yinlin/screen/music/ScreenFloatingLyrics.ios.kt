package love.yinlin.screen.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import love.yinlin.compose.*
import love.yinlin.platform.ActualFloatingLyrics
import love.yinlin.ui.component.input.ProgressSlider
import love.yinlin.ui.component.input.DockedColorPicker
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.layout.SplitLayout
import love.yinlin.service

@Composable
actual fun ScreenFloatingLyrics.ActualContent(device: Device) {
    var iosConfig by rememberRefState { service.config.floatingLyricsIOSConfig }

    Column(modifier = Modifier
        .padding(LocalImmersivePadding.current)
        .fillMaxSize()
        .padding(CustomTheme.padding.equalExtraValue)
        .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
    ) {
        RowLayout("悬浮歌词模式") {
            Switch(
                checked = service.config.enabledFloatingLyrics,
                onCheckedChange = { value ->
                    (service.musicFactory.instance.floatingLyrics as? ActualFloatingLyrics)?.let { floatingLyrics ->
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
                    color = Colors(iosConfig.textColor),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.background(color = Colors(iosConfig.backgroundColor)).padding(CustomTheme.padding.value)
                )
            }
        }

        RowLayout("字体大小") {
            ProgressSlider(
                value = iosConfig.textSizeProgress,
                onValueChange = { iosConfig = iosConfig.copyTextSize(it) },
                onValueChangeFinished = { service.config.floatingLyricsIOSConfig = iosConfig },
                modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.horizontalExtraSpace)
            )
        }

        SplitLayout(
            modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.extraValue),
            horizontalArrangement = CustomTheme.padding.horizontalExtraSpace * 2,
            left = {
                ColumnLayout("字体颜色") {
                    DockedColorPicker(
                        initialColor = Colors(service.config.floatingLyricsIOSConfig.textColor),
                        onColorChanged = { iosConfig = iosConfig.copy(textColor = it.value) },
                        onColorChangeFinished = { service.config.floatingLyricsIOSConfig = iosConfig },
                        modifier = Modifier.widthIn(max = CustomTheme.size.cellWidth).fillMaxWidth()
                    )
                }
            },
            right = {
                ColumnLayout("背景颜色") {
                    DockedColorPicker(
                        initialColor = Colors(service.config.floatingLyricsIOSConfig.backgroundColor),
                        onColorChanged = { iosConfig = iosConfig.copy(backgroundColor = it.value) },
                        onColorChangeFinished = { service.config.floatingLyricsIOSConfig = iosConfig },
                        modifier = Modifier.widthIn(max = CustomTheme.size.cellWidth).fillMaxWidth()
                    )
                }
            }
        )
    }
}