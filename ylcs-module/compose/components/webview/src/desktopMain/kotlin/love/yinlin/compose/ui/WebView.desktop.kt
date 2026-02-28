package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.compose.ui.tool.UnsupportedPlatformComponent

@Composable
actual fun WebView(
    state: WebViewState,
    modifier: Modifier,
    config: WebViewConfig,
) {
    UnsupportedPlatformComponent(modifier = modifier)
}