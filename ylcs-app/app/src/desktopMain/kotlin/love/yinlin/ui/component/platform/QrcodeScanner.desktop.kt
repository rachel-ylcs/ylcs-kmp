package love.yinlin.ui.component.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.platform.UnsupportedPlatformComponent

@Composable
actual fun QrcodeScanner(
    modifier: Modifier,
    onResult: (String) -> Unit
) {
    UnsupportedPlatformComponent(modifier)
}