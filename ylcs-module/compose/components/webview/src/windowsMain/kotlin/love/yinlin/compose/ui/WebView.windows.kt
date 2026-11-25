@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.cinterop.ExperimentalForeignApi
import love.yinlin.compose.ui.tool.NavigationBack
import love.yinlin.compose.ui.tool.UnsupportedPlatformComponent
import love.yinlin.coroutines.Coroutines
import com.microsoft.webview2.interop.rachel_webview_available

@Composable
actual fun WebView(state: WebViewState, modifier: Modifier, config: WebViewConfig) {
    val runtime by produceState<Boolean?>(null, state) {
        value = Coroutines.cpu { rachel_webview_available() }
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
