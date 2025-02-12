package love.yinlin.ui.component.extra

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import love.yinlin.ui.component.DesktopView

@Stable
actual class WebPageState actual constructor(val settings: WebPageSettings) {
	actual var url: String = ""

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
	DesktopView(
		modifier = modifier,
		factory = {
			val jfxPanel = JFXPanel()
			Platform.runLater {
				val webview = WebView()
				webview.engine.isJavaScriptEnabled = state.settings.enableJavaScript
				jfxPanel.scene = Scene(webview)
				webview.engine.load("https://www.baidu.com")
			}
			jfxPanel
		},
		update = {

		}
	)
}