package love.yinlin.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun QrcodeScanner(
    modifier: Modifier,
    onResult: (String) -> Unit
) {
    UnsupportedPlatformComponent(modifier)
}