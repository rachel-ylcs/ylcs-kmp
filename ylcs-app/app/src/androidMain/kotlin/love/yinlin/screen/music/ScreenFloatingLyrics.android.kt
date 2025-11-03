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
import kotlinx.coroutines.delay
import love.yinlin.app
import love.yinlin.compose.*
import love.yinlin.compose.ui.input.ProgressSlider
import love.yinlin.compose.ui.input.DockedColorPicker
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.layout.SplitLayout
import love.yinlin.platform.lyrics.FloatingLyrics

private fun ScreenFloatingLyrics.updateEnabledStatus(floatingLyrics: FloatingLyrics) {
    launch {
        delay(200)
        app.config.enabledFloatingLyrics = floatingLyrics.isAttached
    }
}

private fun ScreenFloatingLyrics.enableFloatingLyrics(value: Boolean) {
    app.mp.floatingLyrics.let {
        if (value) {
            if (it.canAttached) {
                it.attach()
                updateEnabledStatus(it)
            }
            else {
                it.applyPermission { result ->
                    if (result) it.attach()
                    updateEnabledStatus(it)
                }
            }
        }
        else {
            it.detach()
            updateEnabledStatus(it)
        }
    }
}

@Composable
actual fun ScreenFloatingLyrics.platformContent(device: Device) {
    var config by rememberRefState { app.config.lyricsEngineConfig }

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
                onCheckedChange = { enableFloatingLyrics(it) }
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
                        initialColor = Colors(config.textColor),
                        onColorChanged = { config = config.copy(textColor = it.value) },
                        onColorChangeFinished = { app.config.lyricsEngineConfig = config },
                        modifier = Modifier.widthIn(max = CustomTheme.size.cellWidth).fillMaxWidth()
                    )
                }
            },
            right = {
                ColumnLayout("背景颜色") {
                    DockedColorPicker(
                        initialColor = Colors(config.backgroundColor),
                        onColorChanged = { config = config.copy(backgroundColor = it.value) },
                        onColorChangeFinished = { app.config.lyricsEngineConfig = config },
                        modifier = Modifier.widthIn(max = CustomTheme.size.cellWidth).fillMaxWidth()
                    )
                }
            }
        )
    }

    OffScreenEffect {
        app.mp.floatingLyrics.let {
            if (!it.canAttached && it.isAttached) {
                it.detach()
                updateEnabledStatus(it)
            }
        }
    }
}