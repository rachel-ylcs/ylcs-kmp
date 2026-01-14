package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun WebView(
    state: WebViewState,
    modifier: Modifier
) {
    state.Content(modifier)
}