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

	actual abstract fun onRequestIntercepted(url: String, response: String): Boolean
}

//@Stable
//inline fun <T> webPageListener(crossinline listener: (T) -> Unit) =
//	ChangeListener<T> { _, _, value -> value?.let { listener(value) } }
//
//@Stable
//actual class WebPageState actual constructor(val settings: WebPageSettings, initUrl: String) {
//	internal val jfxPanel = mutableStateOf<JFXPanel?>(null)
//	internal var webview: WebView? = null
//
//	internal var mUrl: String by mutableStateOf(initUrl)
//	actual var url: String get() = mUrl
//		set(value) {
//			Platform.runLater {
//				webview?.engine?.load(value)
//			}
//		}
//
//	internal var mLoadingState: WebPageLoadingState by mutableStateOf(WebPageLoadingState.Initializing)
//	actual val loadingState: WebPageLoadingState get() = mLoadingState
//	internal val loadingStateListener = webPageListener<Worker.State> {
//		if (it == Worker.State.SUCCEEDED) mLoadingState = WebPageLoadingState.Finished
//	}
//	internal val progressListener = webPageListener<Number> {
//		val progress = it.toFloat()
//		if (progress > 0f) mLoadingState = WebPageLoadingState.Loading(progress)
//	}
//
//	internal var mTitle: String by mutableStateOf("")
//	actual val title: String get() = mTitle
//	internal val titleListener = webPageListener<String> { mTitle = it }
//
//	actual val icon: BitmapPainter? get() = null
//
//	internal var mCanGoBack: Boolean by mutableStateOf(false)
//	actual val canGoBack: Boolean get() = mCanGoBack
//
//	internal var mCanGoForward: Boolean by mutableStateOf(false)
//	actual val canGoForward: Boolean get() = mCanGoForward
//	internal val historyListener = webPageListener<Number> {
//		webview?.engine?.history?.let { history ->
//			val currentIndex = it.toInt()
//			mCanGoBack = currentIndex > 0
//			mCanGoForward = currentIndex < history.entries.size - 1
//		}
//	}
//
//	internal var mError: WebPageError? by mutableStateOf(null)
//	actual val error: WebPageError? get() = mError
//	internal val errorListener = EventHandler<WebErrorEvent> { mError = WebPageError(0, it.message) }
//
//	actual fun goBack() {
//		Platform.runLater {
//			webview?.engine?.history?.let {
//				if (it.currentIndex > 0) it.go(-1)
//			}
//		}
//	}
//
//	actual fun goForward() {
//		Platform.runLater {
//			webview?.engine?.history?.let {
//				if (it.currentIndex < it.maxSize - 1) it.go(1)
//			}
//		}
//	}
//
//	actual fun evaluateJavaScript(script: String) {
//		Platform.runLater {
//			webview?.engine?.executeScript(script)
//		}
//	}
//}
//
//@Composable
//actual fun WebPage(
//	state: WebPageState,
//	modifier: Modifier
//) {
//	CustomUI(
//		view = state.jfxPanel,
//		modifier = modifier,
//		factory = {
//			val jfxPanel = JFXPanel()
//			Platform.runLater {
//				state.webview = WebView().apply {
//					engine?.apply {
//						isJavaScriptEnabled = state.settings.enableJavaScript
//
//						loadWorker.apply {
//							stateProperty().addListener(state.loadingStateListener)
//							progressProperty().addListener(state.progressListener)
//						}
//						history.currentIndexProperty().addListener(state.historyListener)
//						titleProperty().addListener(state.titleListener)
//						onError = state.errorListener
//
//						if (state.mUrl.isNotEmpty()) load(state.mUrl)
//					}
//					jfxPanel.scene = Scene(StackPane(this))
//				}
//				jfxPanel.background = java.awt.Color.WHITE
//			}
//			jfxPanel
//		},
//		release = { jfxPanel, onRelease ->
//			Platform.runLater {
//				state.webview?.let { webview ->
//					webview.engine?.apply {
//						loadWorker.apply {
//							stateProperty().removeListener(state.loadingStateListener)
//							progressProperty().removeListener(state.progressListener)
//							cancel()
//						}
//						history.currentIndexProperty().removeListener(state.historyListener)
//						titleProperty().removeListener(state.titleListener)
//						onError = EventHandler { }
//						load(null)
//					}
//					(jfxPanel.scene?.root as? StackPane)?.children?.clear()
//					state.webview = null
//				}
//				jfxPanel.scene = null
//				onRelease()
//			}
//		}
//	)
//}