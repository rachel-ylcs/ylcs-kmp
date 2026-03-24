package love.yinlin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.app
import love.yinlin.compose.OffScreenEffect
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.input.Slider
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.media.lyrics.LyricsEngineConfig

@Composable
actual fun ScreenLyricsSettings.PlatformContent() {
    mp?.floatingLyrics?.let { floatingLyrics ->
        OffScreenEffect { isForeground ->
            if (isForeground) floatingLyrics.check()
        }

        LyricsSwitch(modifier = Modifier.fillMaxWidth(), onCheckedChange = {
            app.config.enabledFloatingLyrics = it
            floatingLyrics.check()
        })

        LyricsPreview(modifier = Modifier.fillMaxWidth())

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleEllipsisText("左侧偏移", style = Theme.typography.v7.bold)
            Slider(
                value = config.android.leftProgress,
                onValueChange = { config = config.copy(android = config.android.copyLeft(it)) },
                onValueChangeFinished = { app.config.lyricsEngineConfig = config },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleEllipsisText("右侧偏移", style = Theme.typography.v7.bold)
            Slider(
                value = config.android.rightProgress,
                onValueChange = { config = config.copy(android = config.android.copyRight(it)) },
                onValueChangeFinished = { app.config.lyricsEngineConfig = config },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleEllipsisText("垂直方向", style = Theme.typography.v7.bold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SimpleEllipsisText(if (config.android.placeTop) "顶部" else "底部")
                Switch(checked = config.android.placeTop, onCheckedChange = {
                    config = config.copy(android = config.android.copy(placeTop = it))
                    app.config.lyricsEngineConfig = config
                })
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleEllipsisText("顶部偏移", style = Theme.typography.v7.bold)
            Slider(
                value = config.android.topProgress,
                onValueChange = { config = config.copy(android = config.android.copyTop(it)) },
                onValueChangeFinished = { app.config.lyricsEngineConfig = config },
                modifier = Modifier.weight(1f)
            )
        }

        LyricsFontSizeLayout(modifier = Modifier.fillMaxWidth())
        LyricsColorLayout(modifier = Modifier.fillMaxWidth())
    }
}

actual fun ScreenLyricsSettings.resetLyricsSettings(newConfig: LyricsEngineConfig) {
    app.config.lyricsEngineConfig = newConfig
}