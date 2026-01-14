package love.yinlin.screen.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.app
import love.yinlin.compose.*
import love.yinlin.compose.ui.CustomTheme
import love.yinlin.compose.ui.input.ProgressSlider
import love.yinlin.compose.ui.input.DockedColorPicker
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.layout.SplitLayout

@Composable
actual fun ScreenLyricsSettings.PlatformContent() {
    RowLayout("悬浮歌词模式") {
        Switch(
            checked = app.config.enabledFloatingLyrics,
            onCheckedChange = { value ->
                app.config.enabledFloatingLyrics = value
                if (app.mp.floatingLyrics.canAttached) {
                    app.mp.floatingLyrics.check()
                } else {
                    // 模拟器和旧机型不支持画中画能力
                    slot.tip.error("该设备不支持悬浮歌词")
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
                    fontSize = MaterialTheme.typography.labelLarge.fontSize * config.textSize
                ),
                color = Colors(config.textColor),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.background(color = Colors(config.backgroundColor)).padding(CustomTheme.padding.value)
            )
        }
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
                    initialColor = remember { Colors(config.textColor) },
                    onColorChanged = { config = config.copy(textColor = it.value) },
                    onColorChangeFinished = { app.config.lyricsEngineConfig = config },
                    modifier = Modifier.widthIn(max = CustomTheme.size.cellWidth).fillMaxWidth()
                )
            }
        },
        right = {
            ColumnLayout("背景颜色") {
                DockedColorPicker(
                    initialColor = remember { Colors(config.backgroundColor) },
                    onColorChanged = { config = config.copy(backgroundColor = it.value) },
                    onColorChangeFinished = { app.config.lyricsEngineConfig = config },
                    modifier = Modifier.widthIn(max = CustomTheme.size.cellWidth).fillMaxWidth()
                )
            }
        }
    )
}