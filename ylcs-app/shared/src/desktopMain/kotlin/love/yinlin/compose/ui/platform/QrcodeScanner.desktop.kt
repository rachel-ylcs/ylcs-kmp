package love.yinlin.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.compose.ui.layout.UnsupportedPlatformComponent

@Composable
actual fun QrcodeScanner(
    modifier: Modifier,
    onAlbumPick: suspend () -> ByteArray?,
    onResult: (String) -> Unit
) {
    UnsupportedPlatformComponent(modifier)
}