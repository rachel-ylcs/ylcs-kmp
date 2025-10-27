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
import love.yinlin.compose.*
import love.yinlin.platform.ActualFloatingLyrics
import love.yinlin.platform.FloatingLyrics
import love.yinlin.compose.ui.input.ProgressSlider
import love.yinlin.compose.ui.input.DockedColorPicker
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.layout.SplitLayout
import love.yinlin.service

private fun ScreenFloatingLyrics.updateEnabledStatus(floatingLyrics: FloatingLyrics) {
    launch {
        delay(200)
        service.config.enabledFloatingLyrics = floatingLyrics.isAttached
    }
}

private fun ScreenFloatingLyrics.enableFloatingLyrics(value: Boolean) {
    (service.musicFactory.instance.floatingLyrics as? ActualFloatingLyrics)?.let { floatingLyrics ->
        if (value) {
            if (floatingLyrics.canAttached) {
                floatingLyrics.attach()
                updateEnabledStatus(floatingLyrics)
            }
            else {
                floatingLyrics.applyPermission {
                    if (it) floatingLyrics.attach()
                    updateEnabledStatus(floatingLyrics)
                }
            }
        }
        else {
            floatingLyrics.detach()
            updateEnabledStatus(floatingLyrics)
        }
    }
}

@Composable
actual fun ScreenFloatingLyrics.ActualContent(device: Device) {
    var androidConfig by rememberRefState { service.config.floatingLyricsAndroidConfig }

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
                onCheckedChange = { enableFloatingLyrics(it) }
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.padding(
                    start = this.maxWidth * androidConfig.left.coerceIn(0f, 1f),
                    end = this.maxWidth * (1 - androidConfig.right).coerceIn(0f, 1f),
                    top = CustomTheme.padding.verticalExtraSpace * 4f * androidConfig.top
                ).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "这是一条测试歌词~",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = MaterialTheme.typography.labelLarge.fontSize * androidConfig.textSize
                    ),
                    color = Colors(androidConfig.textColor),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.background(color = Colors(androidConfig.backgroundColor)).padding(CustomTheme.padding.value)
                )
            }
        }

        RowLayout("左侧偏移") {
            ProgressSlider(
                value = androidConfig.leftProgress,
                onValueChange = { androidConfig = androidConfig.copyLeft(it) },
                onValueChangeFinished = { service.config.floatingLyricsAndroidConfig = androidConfig },
                modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.horizontalExtraSpace)
            )
        }

        RowLayout("右侧偏移") {
            ProgressSlider(
                value = androidConfig.rightProgress,
                onValueChange = { androidConfig = androidConfig.copyRight(it) },
                onValueChangeFinished = { service.config.floatingLyricsAndroidConfig = androidConfig },
                modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.horizontalExtraSpace)
            )
        }

        RowLayout("顶部偏移") {
            ProgressSlider(
                value = androidConfig.topProgress,
                onValueChange = { androidConfig = androidConfig.copyTop(it) },
                onValueChangeFinished = { service.config.floatingLyricsAndroidConfig = androidConfig },
                modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.horizontalExtraSpace)
            )
        }

        RowLayout("字体大小") {
            ProgressSlider(
                value = androidConfig.textSizeProgress,
                onValueChange = { androidConfig = androidConfig.copyTextSize(it) },
                onValueChangeFinished = { service.config.floatingLyricsAndroidConfig = androidConfig },
                modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.horizontalExtraSpace)
            )
        }

        SplitLayout(
            modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.extraValue),
            horizontalArrangement = CustomTheme.padding.horizontalExtraSpace * 2,
            left = {
                ColumnLayout("字体颜色") {
                    DockedColorPicker(
                        initialColor = Colors(service.config.floatingLyricsAndroidConfig.textColor),
                        onColorChanged = { androidConfig = androidConfig.copy(textColor = it.value) },
                        onColorChangeFinished = { service.config.floatingLyricsAndroidConfig = androidConfig },
                        modifier = Modifier.widthIn(max = CustomTheme.size.cellWidth).fillMaxWidth()
                    )
                }
            },
            right = {
                ColumnLayout("背景颜色") {
                    DockedColorPicker(
                        initialColor = Colors(service.config.floatingLyricsAndroidConfig.backgroundColor),
                        onColorChanged = { androidConfig = androidConfig.copy(backgroundColor = it.value) },
                        onColorChangeFinished = { service.config.floatingLyricsAndroidConfig = androidConfig },
                        modifier = Modifier.widthIn(max = CustomTheme.size.cellWidth).fillMaxWidth()
                    )
                }
            }
        )
    }

    OffScreenEffect {
        (service.musicFactory.instance.floatingLyrics as? ActualFloatingLyrics)?.let { floatingLyrics ->
            if (!floatingLyrics.canAttached && floatingLyrics.isAttached) {
                floatingLyrics.detach()
                updateEnabledStatus(floatingLyrics)
            }
        }
    }
}