@file:JvmName("AndroidScreenFloatingLyrics")
package love.yinlin.ui.screen.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import love.yinlin.common.Colors
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.extension.OffScreenEffect
import love.yinlin.extension.rememberState
import love.yinlin.platform.ActualFloatingLyrics
import love.yinlin.platform.FloatingLyrics
import love.yinlin.platform.app
import love.yinlin.ui.component.input.DockedColorPicker
import love.yinlin.ui.component.input.ProgressSlider
import love.yinlin.ui.component.input.Switch
import love.yinlin.ui.component.layout.SplitLayout

private fun ScreenFloatingLyrics.updateEnabledStatus(floatingLyrics: FloatingLyrics) {
    launch {
        delay(200)
        app.config.enabledFloatingLyrics = floatingLyrics.isAttached
    }
}

private fun ScreenFloatingLyrics.enableFloatingLyrics(value: Boolean) {
    (app.musicFactory.floatingLyrics as? ActualFloatingLyrics)?.let { floatingLyrics ->
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
    var androidConfig by rememberState { app.config.floatingLyricsAndroidConfig }

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
                onCheckedChange = { enableFloatingLyrics(it) }
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.padding(
                    start = this.maxWidth * androidConfig.left.coerceIn(0f, 1f),
                    end = this.maxWidth * (1 - androidConfig.right).coerceIn(0f, 1f),
                    top = ThemeValue.Padding.VerticalExtraSpace * 4f * androidConfig.top
                ).fillMaxWidth(),
                contentAlignment = Center
            ) {
                Text(
                    text = "这是一条测试歌词~",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = MaterialTheme.typography.labelLarge.fontSize * androidConfig.textSize
                    ),
                    color = Colors.from(androidConfig.textColor),
                    textAlign = Center,
                    maxLines = 2,
                    overflow = Ellipsis,
                    modifier = Modifier.background(color = Colors.from(androidConfig.backgroundColor)).padding(ThemeValue.Padding.Value)
                )
            }
        }

        RowLayout("左侧偏移") {
            ProgressSlider(
                value = androidConfig.leftProgress,
                onValueChange = { androidConfig = androidConfig.copyLeft(it) },
                onValueChangeFinished = { app.config.floatingLyricsAndroidConfig = androidConfig },
                modifier = Modifier.fillMaxWidth().padding(horizontal = ThemeValue.Padding.HorizontalExtraSpace)
            )
        }

        RowLayout("右侧偏移") {
            ProgressSlider(
                value = androidConfig.rightProgress,
                onValueChange = { androidConfig = androidConfig.copyRight(it) },
                onValueChangeFinished = { app.config.floatingLyricsAndroidConfig = androidConfig },
                modifier = Modifier.fillMaxWidth().padding(horizontal = ThemeValue.Padding.HorizontalExtraSpace)
            )
        }

        RowLayout("顶部偏移") {
            ProgressSlider(
                value = androidConfig.topProgress,
                onValueChange = { androidConfig = androidConfig.copyTop(it) },
                onValueChangeFinished = { app.config.floatingLyricsAndroidConfig = androidConfig },
                modifier = Modifier.fillMaxWidth().padding(horizontal = ThemeValue.Padding.HorizontalExtraSpace)
            )
        }

        RowLayout("字体大小") {
            ProgressSlider(
                value = androidConfig.textSizeProgress,
                onValueChange = { androidConfig = androidConfig.copyTextSize(it) },
                onValueChangeFinished = { app.config.floatingLyricsAndroidConfig = androidConfig },
                modifier = Modifier.fillMaxWidth().padding(horizontal = ThemeValue.Padding.HorizontalExtraSpace)
            )
        }

        SplitLayout(
            modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.ExtraValue),
            horizontalArrangement = ThemeValue.Padding.HorizontalExtraSpace * 2,
            left = {
                ColumnLayout("字体颜色") {
                    DockedColorPicker(
                        initialColor = Colors.from(app.config.floatingLyricsAndroidConfig.textColor),
                        onColorChanged = { androidConfig = androidConfig.copy(textColor = it.value) },
                        onColorChangeFinished = { app.config.floatingLyricsAndroidConfig = androidConfig },
                        modifier = Modifier.widthIn(max = ThemeValue.Size.CellWidth).fillMaxWidth()
                    )
                }
            },
            right = {
                ColumnLayout("背景颜色") {
                    DockedColorPicker(
                        initialColor = Colors.from(app.config.floatingLyricsAndroidConfig.backgroundColor),
                        onColorChanged = { androidConfig = androidConfig.copy(backgroundColor = it.value) },
                        onColorChangeFinished = { app.config.floatingLyricsAndroidConfig = androidConfig },
                        modifier = Modifier.widthIn(max = ThemeValue.Size.CellWidth).fillMaxWidth()
                    )
                }
            }
        )
    }

    OffScreenEffect {
        (app.musicFactory.floatingLyrics as? ActualFloatingLyrics)?.let { floatingLyrics ->
            if (!floatingLyrics.canAttached && floatingLyrics.isAttached) {
                floatingLyrics.detach()
                updateEnabledStatus(floatingLyrics)
            }
        }
    }
}