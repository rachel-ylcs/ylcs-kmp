@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import love.yinlin.compose.ui.tool.NavigationBack
import love.yinlin.compose.ui.tool.UnsupportedPlatformComponent
import love.yinlin.webview.internal.rachel_webview_available

@Composable
actual fun WebView(state: WebViewState, modifier: Modifier, config: WebViewConfig) {
    val runtime by produceState<Boolean?>(null, state) {
        value = withContext(Dispatchers.Default) { rachel_webview_available() }
    }
    when {
        runtime == null -> Box(modifier)
        runtime != true || !state.available -> UnsupportedPlatformComponent(modifier)
        else -> {
            NavigationBack(enabled = state.canGoBack) { state.goBack() }
            PlatformViewHost(state, WebViewParameters(config), modifier)
        }
    }
}
