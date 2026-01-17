package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.compose.ui.layout.NavigationBack

@Composable
actual fun WebView(
    state: WebViewState,
    modifier: Modifier,
    config: WebViewConfig,
) {
    NavigationBack(enabled = state.canGoBack) { state.goBack() }

    state.HostView(modifier = modifier)

    state.Monitor(config) {
        it.settings.apply {
            javaScriptEnabled = config.enableJavaScript
            javaScriptCanOpenWindowsAutomatically = config.enableJavaScriptOpenWindow
            domStorageEnabled = config.enableDomStorage
            allowFileAccess = config.enableFileAccess
            allowContentAccess = config.enableContentAccess
        }
    }
}