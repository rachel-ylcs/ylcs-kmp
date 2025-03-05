package love.yinlin.ui.component.extra

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.web.WebErrorEvent
import javafx.scene.web.WebView
import love.yinlin.ui.component.CustomUI

@Stable
inline fun <T> webPageListener(crossinline listener: (T) -> Unit) =
	ChangeListener<T> { _, _, value -> value?.let { listener(value) } }

@Stable
actual class WebPageState actual constructor(val settings: WebPageSettings, initUrl: String) {
	internal val jfxPanel = mutableStateOf<JFXPanel?>(null)
	private val webview: WebView? get() = jfxPanel.value?.scene?.root as? WebView

	internal var mUrl: String by mutableStateOf(initUrl)
	actual var url: String get() = mUrl
		set(value) {
			Platform.runLater {
				webview?.engine?.load(value)
			}
		}

	internal var mLoadingState: WebPageLoadingState by mutableStateOf(WebPageLoadingState.Initializing)
	actual val loadingState: WebPageLoadingState get() = mLoadingState
	internal val loadingStateListener = webPageListener<Worker.State> {
		if (it == Worker.State.SUCCEEDED) mLoadingState = WebPageLoadingState.Finished
	}
	internal val progressListener = webPageListener<Number> {
		val progress = it.toFloat()
		if (progress > 0f) mLoadingState = WebPageLoadingState.Loading(progress)
	}

	internal var mTitle: String by mutableStateOf("")
	actual val title: String get() = mTitle
	internal val titleListener = webPageListener<String> { mTitle = it }

	actual val icon: BitmapPainter? get() = null

	internal var mCanGoBack: Boolean by mutableStateOf(false)
	actual val canGoBack: Boolean get() = mCanGoBack

	internal var mCanGoForward: Boolean by mutableStateOf(false)
	actual val canGoForward: Boolean get() = mCanGoForward
	internal val historyListener = webPageListener<Number> {
		webview?.engine?.history?.let { history ->
			val currentIndex = it.toInt()
			mCanGoBack = currentIndex > 0
			mCanGoForward = currentIndex < history.entries.size - 1
		}
	}

	internal var mError: WebPageError? by mutableStateOf(null)
	actual val error: WebPageError? get() = mError
	internal val errorListener = EventHandler<WebErrorEvent> { mError = WebPageError(0, it.message) }

	actual fun goBack() {
		Platform.runLater {
			webview?.engine?.history?.let {
				if (it.currentIndex > 0) it.go(-1)
			}
		}
	}

	actual fun goForward() {
		Platform.runLater {
			webview?.engine?.history?.let {
				if (it.currentIndex < it.maxSize - 1) it.go(1)
			}
		}
	}

	actual fun evaluateJavaScript(script: String) {
		Platform.runLater {
			webview?.engine?.executeScript(script)
		}
	}
}

@Composable
actual fun WebPage(
	state: WebPageState,
	modifier: Modifier
) {
	CustomUI(
		view = state.jfxPanel,
		modifier = modifier,
		factory = {
			val jfxPanel = JFXPanel()
			Platform.runLater {
				val webview = WebView()
				webview.engine?.apply {
					isJavaScriptEnabled = state.settings.enableJavaScript

					loadWorker.apply {
						stateProperty().addListener(state.loadingStateListener)
						progressProperty().addListener(state.progressListener)
					}
					history.currentIndexProperty().addListener(state.historyListener)
					onError = state.errorListener
					titleProperty().addListener(state.titleListener)

					if (state.mUrl.isNotEmpty()) load(state.mUrl)
				}
				jfxPanel.scene = Scene(webview)
				jfxPanel.background = java.awt.Color.WHITE
			}
			jfxPanel
		},
		release = {
			Platform.runLater {
				(it.scene?.root as? WebView)?.engine?.apply {
					loadWorker.apply {
						stateProperty().removeListener(state.loadingStateListener)
						progressProperty().removeListener(state.progressListener)
					}
					history.currentIndexProperty().removeListener(state.historyListener)
					onError = EventHandler { }
					titleProperty().removeListener(state.titleListener)
				}
				it.scene = null
			}
		}
	)
}