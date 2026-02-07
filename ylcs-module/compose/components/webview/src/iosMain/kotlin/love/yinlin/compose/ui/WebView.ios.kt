package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.compose.ui.tool.NavigationBack
import platform.WebKit.javaScriptEnabled

@Composable
actual fun WebView(
    state: WebViewState,
    modifier: Modifier,
    config: WebViewConfig,
) {
    NavigationBack(enabled = state.canGoBack) { state.goBack() }

    state.HostView(modifier = modifier)

    state.Monitor(config) {
        it.configuration.apply {
            defaultWebpagePreferences.allowsContentJavaScript = config.enableJavaScript
            preferences.javaScriptEnabled = config.enableJavaScript
            preferences.javaScriptCanOpenWindowsAutomatically = config.enableJavaScriptOpenWindow
        }
    }
}