package love.yinlin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.app

// TODO: ios部分需要重新实现, 把 platform 相关的代码全部移入 swift
// 涉及到 view、初始化、布局、attach 等等都放在原生上
@Composable
actual fun ScreenLyricsSettings.PlatformContent() {
    mp?.floatingLyrics?.let { floatingLyrics ->
        LyricsSwitch(modifier = Modifier.fillMaxWidth(), onCheckedChange = {
            app.config.enabledFloatingLyrics = it
            floatingLyrics.check()
        })

        LyricsFontSizeLayout(modifier = Modifier.fillMaxWidth())
        LyricsColorLayout(modifier = Modifier.fillMaxWidth())
    }
}