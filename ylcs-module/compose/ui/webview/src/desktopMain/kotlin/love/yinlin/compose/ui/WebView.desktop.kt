package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.compose.ui.layout.UnsupportedPlatformComponent

@Composable
actual fun WebView(
    state: WebViewState,
    modifier: Modifier
) {
    UnsupportedPlatformComponent(modifier = modifier)
}