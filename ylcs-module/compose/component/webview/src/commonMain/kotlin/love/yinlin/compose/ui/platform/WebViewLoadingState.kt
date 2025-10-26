package love.yinlin.compose.ui.platform

import androidx.compose.runtime.Stable

@Stable
sealed interface WebViewLoadingState {
    data object Initializing: WebViewLoadingState // 初始化
    data object Finished: WebViewLoadingState // 已完成
    data class Loading(val progress: Float): WebViewLoadingState // 加载中
}