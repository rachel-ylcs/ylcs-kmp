package love.yinlin.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import love.yinlin.app

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