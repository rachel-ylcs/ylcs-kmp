package love.yinlin.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import love.yinlin.app
import love.yinlin.media.lyrics.LyricsEngineConfig

@Composable
actual fun ScreenLyricsSettings.PlatformContent() {
    mp?.floatingLyrics?.let { floatingLyrics ->
        DisposableEffect(Unit) {
            config = app.config.lyricsEngineConfig
            floatingLyrics.canMove = true
            onDispose { floatingLyrics.canMove = false }
        }

        LyricsSwitch(modifier = Modifier.fillMaxWidth(), onCheckedChange = {
            app.config.enabledFloatingLyrics = it
            floatingLyrics.check()
        })

        LyricsPreview(modifier = Modifier.fillMaxWidth())

        LyricsFontSizeLayout(modifier = Modifier.fillMaxWidth())

        LyricsColorLayout(modifier = Modifier.fillMaxWidth())
    }
}

actual fun ScreenLyricsSettings.resetLyricsSettings(newConfig: LyricsEngineConfig) {
    app.config.lyricsEngineConfig = newConfig

    // 触发桌面悬浮歌词修改窗口尺寸和位置
    val desktopConfig = newConfig.desktop
    mp?.floatingLyrics?.updateWindowState(
        size = DpSize(desktopConfig.width.dp, desktopConfig.height.dp),
        position = WindowPosition(desktopConfig.x.dp, desktopConfig.y.dp)
    )
}