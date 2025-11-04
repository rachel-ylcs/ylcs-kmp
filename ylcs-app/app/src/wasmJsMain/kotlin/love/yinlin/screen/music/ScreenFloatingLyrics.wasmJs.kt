package love.yinlin.screen.music

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.compose.Device
import love.yinlin.compose.ui.platform.UnsupportedPlatformComponent

@Composable
actual fun ScreenFloatingLyrics.platformContent(device: Device) {
    UnsupportedPlatformComponent(modifier = Modifier.fillMaxSize())
}