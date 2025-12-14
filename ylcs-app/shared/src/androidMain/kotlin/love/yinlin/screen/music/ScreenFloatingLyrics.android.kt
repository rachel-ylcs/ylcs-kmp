package love.yinlin.screen.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import love.yinlin.app
import love.yinlin.compose.*
import love.yinlin.compose.ui.input.ProgressSlider
import love.yinlin.compose.ui.input.DockedColorPicker
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.layout.SplitLayout

@Composable
actual fun ScreenLyricsSettings.PlatformContent(device: Device) {
    Column(modifier = Modifier
        .padding(LocalImmersivePadding.current)
        .fillMaxSize()
        .padding(CustomTheme.padding.equalExtraValue)
        .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
    ) {
        RowLayout("悬浮歌词模式") {
            Switch(
                checked = app.config.enabledFloatingLyrics,
                onCheckedChange = {
                    app.config.enabledFloatingLyrics = it
                    app.mp.floatingLyrics.check()
                }
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.padding(
                    start = this.maxWidth * config.android.left.coerceIn(0f, 1f),
                    end = this.maxWidth * (1 - config.android.right).coerceIn(0f, 1f),
                    top = CustomTheme.padding.verticalExtraSpace * 4f * config.android.top
                ).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
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
        }

        RowLayout("左侧偏移") {
            ProgressSlider(
                value = config.android.leftProgress,
                onValueChange = { config = config.copy(android = config.android.copyLeft(it)) },
                onValueChangeFinished = { app.config.lyricsEngineConfig = config },
                modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.horizontalExtraSpace)
            )
        }

        RowLayout("右侧偏移") {
            ProgressSlider(
                value = config.android.rightProgress,
                onValueChange = { config = config.copy(android = config.android.copyRight(it)) },
                onValueChangeFinished = { app.config.lyricsEngineConfig = config },
                modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.horizontalExtraSpace)
            )
        }

        RowLayout("顶部偏移") {
            ProgressSlider(
                value = config.android.topProgress,
                onValueChange = { config = config.copy(android = config.android.copyTop(it)) },
                onValueChangeFinished = { app.config.lyricsEngineConfig = config },
                modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.horizontalExtraSpace)
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

    OffScreenEffect { isForeground ->
        if (isForeground) app.mp.floatingLyrics.check()
    }
}