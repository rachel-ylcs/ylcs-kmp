package love.yinlin.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.app
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.input.ColorPicker
import love.yinlin.compose.ui.input.Slider
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.text.SimpleEllipsisText

@Composable
actual fun ScreenLyricsSettings.PlatformContent() {
    mp?.floatingLyrics?.let { floatingLyrics ->
        DisposableEffect(Unit) {
            config = app.config.lyricsEngineConfig
            floatingLyrics.canMove = true
            onDispose { floatingLyrics.canMove = false }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleEllipsisText("悬浮歌词模式", style = Theme.typography.v7.bold)
            Switch(
                checked = app.config.enabledFloatingLyrics,
                onCheckedChange = {
                    app.config.enabledFloatingLyrics = it
                    floatingLyrics.check()
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleEllipsisText("字体大小", style = Theme.typography.v7.bold)
            Slider(
                value = config.textSizeProgress,
                onValueChange = { config = config.copyTextSize(it) },
                onValueChangeFinished = { app.config.lyricsEngineConfig = config },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                SimpleEllipsisText("字体颜色", style = Theme.typography.v7.bold)
                ColorPicker(
                    initColor = Colors(app.config.lyricsEngineConfig.textColor),
                    onColorChanged = { config = config.copy(textColor = it.value) },
                    onColorChangeFinished = { app.config.lyricsEngineConfig = config },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                SimpleEllipsisText("背景颜色", style = Theme.typography.v7.bold)
                ColorPicker(
                    initColor = Colors(app.config.lyricsEngineConfig.backgroundColor),
                    onColorChanged = { config = config.copy(backgroundColor = it.value) },
                    onColorChangeFinished = { app.config.lyricsEngineConfig = config },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}