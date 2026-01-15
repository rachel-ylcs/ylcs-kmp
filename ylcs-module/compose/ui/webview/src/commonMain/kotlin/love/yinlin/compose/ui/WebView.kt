package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
expect fun WebView(
    state: WebViewState,
    modifier: Modifier = Modifier,
    config: WebViewConfig = remember { WebViewConfig() },
)