package love.yinlin.ui.component.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter

@Stable
data class WebPageSettings(
	val enableJavaScript: Boolean = true, // 开启 JavaScript
	val enableJavaScriptOpenWindow: Boolean = true, // 开启 JavaScript 打开窗口
	val enableDomStorage: Boolean = true, // 允许 DOM 存储
	val enableFileAccess: Boolean = true, // 允许文件访问
	val enableContentAccess: Boolean = true, // 允许内容访问
)

@Stable
sealed interface WebPageLoadingState {
	data object Initializing: WebPageLoadingState // 初始化
	data object Finished: WebPageLoadingState // 已完成
	data class Loading(val progress: Float): WebPageLoadingState // 加载中
}

@Stable
data class WebPageError(
	val code: Long,
	val description: String
)

@Stable
expect class WebPageState(settings: WebPageSettings, initUrl: String = "") {
	var url: String // URL

	val loadingState: WebPageLoadingState // 加载状态
	val title: String // 标题
	val icon: BitmapPainter? // 图标

	val canGoBack: Boolean // 是否可返回
	val canGoForward: Boolean // 是否可前进

	val error: WebPageError? // 网页错误

	fun goBack() // 返回
	fun goForward() // 前进
	fun evaluateJavaScript(script: String) // 执行 JavaScript
}

@Composable
expect fun WebPage(
	state: WebPageState,
	modifier: Modifier = Modifier
)