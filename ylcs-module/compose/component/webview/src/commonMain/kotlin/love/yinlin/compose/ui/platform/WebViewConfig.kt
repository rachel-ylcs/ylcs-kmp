package love.yinlin.compose.ui.platform

import androidx.compose.runtime.Stable

@Stable
data class WebViewConfig(
    val enableJavaScript: Boolean = true, // 开启 JavaScript
    val enableJavaScriptOpenWindow: Boolean = true, // 开启 JavaScript 打开窗口
    val enableDomStorage: Boolean = true, // 允许 DOM 存储
    val enableFileAccess: Boolean = true, // 允许文件访问
    val enableContentAccess: Boolean = true, // 允许内容访问
)