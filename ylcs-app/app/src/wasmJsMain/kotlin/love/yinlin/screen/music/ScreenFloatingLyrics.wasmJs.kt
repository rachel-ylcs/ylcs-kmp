package love.yinlin.screen.music

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.compose.Device
import love.yinlin.platform.UnsupportedPlatformComponent

@Composable
actual fun ScreenFloatingLyrics.ActualContent(device: Device) {
    UnsupportedPlatformComponent(modifier = Modifier.fillMaxSize())
}