package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.BitmapPainter

@Stable
expect class WebViewState(settings: WebViewConfig, initUrl: String = "") {
    var url: String // URL

    val loadingState: WebViewLoadingState // 加载状态
    val title: String // 标题
    val icon: BitmapPainter? // 图标

    val canGoBack: Boolean // 是否可返回
    val canGoForward: Boolean // 是否可前进

    val error: WebViewError? // 网页错误

    fun goBack() // 返回
    fun goForward() // 前进
    fun evaluateJavaScript(script: String) // 执行 JavaScript
}