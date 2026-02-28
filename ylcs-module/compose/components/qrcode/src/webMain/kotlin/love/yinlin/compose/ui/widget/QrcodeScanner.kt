package love.yinlin.compose.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.compose.ui.tool.UnsupportedPlatformComponent

@Composable
actual fun QrcodeScanner(
    modifier: Modifier,
    onData: suspend () -> ByteArray?,
    onResult: (String) -> Unit
) {
    UnsupportedPlatformComponent(modifier)
}