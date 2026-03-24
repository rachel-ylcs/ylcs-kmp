package love.yinlin.screen

import androidx.compose.runtime.Composable
import love.yinlin.app
import love.yinlin.media.lyrics.LyricsEngineConfig

@Composable
actual fun ScreenLyricsSettings.PlatformContent() { }

actual fun ScreenLyricsSettings.resetLyricsSettings(newConfig: LyricsEngineConfig) {
    app.config.lyricsEngineConfig = newConfig
}