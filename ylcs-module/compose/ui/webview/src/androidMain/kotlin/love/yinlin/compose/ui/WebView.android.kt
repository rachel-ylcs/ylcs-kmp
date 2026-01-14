package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.compose.ui.layout.NavigationBack

@Composable
actual fun WebView(
    state: WebViewState,
    modifier: Modifier
) {
    NavigationBack(enabled = state.canGoBack) {
        state.goBack()
    }
    state.Content(modifier)
}