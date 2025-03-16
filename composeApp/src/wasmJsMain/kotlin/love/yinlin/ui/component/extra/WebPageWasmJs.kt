package love.yinlin.ui.component.extra

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import love.yinlin.ui.component.common.UnsupportedComponent

@Stable
actual class WebPageState actual constructor(val settings: WebPageSettings, initUrl: String) {
	actual var url: String = initUrl

	actual val loadingState: WebPageLoadingState get() = WebPageLoadingState.Initializing

	actual val title: String get() = ""

	actual val icon: BitmapPainter? get() = null

	actual val canGoBack: Boolean get() = false

	actual val canGoForward: Boolean get() = false

	actual val error: WebPageError? get() = null

	actual fun goBack() { }
	actual fun goForward() { }
	actual fun evaluateJavaScript(script: String) { }
}

@Composable
actual fun WebPage(
	state: WebPageState,
	modifier: Modifier
) {
	UnsupportedComponent(modifier = modifier)
}