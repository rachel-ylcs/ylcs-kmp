package love.yinlin.ui.component.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import love.yinlin.platform.UnsupportedComponent

@Stable
actual class WebPageState actual constructor(val settings: WebPageSettings, initUrl: String) {
	actual var url: String = initUrl
	actual val loadingState: WebPageLoadingState = WebPageLoadingState.Initializing
	actual val title: String = ""
	actual val icon: BitmapPainter? = null
	actual val canGoBack: Boolean = false
	actual val canGoForward: Boolean = false
	actual val error: WebPageError? = null
	actual fun goBack() {}
	actual fun goForward() {}
	actual fun evaluateJavaScript(script: String) {}
}

@Composable
actual fun WebPage(
	state: WebPageState,
	modifier: Modifier
) {
	UnsupportedComponent(modifier = modifier)
}

@Stable
actual abstract class HeadlessBrowser actual constructor() {
	actual fun load(url: String) {}

	actual fun destroy() {}

	actual abstract fun onUrlIntercepted(url: String): Boolean

	actual abstract fun onRequestIntercepted(url: String, response: String)
}