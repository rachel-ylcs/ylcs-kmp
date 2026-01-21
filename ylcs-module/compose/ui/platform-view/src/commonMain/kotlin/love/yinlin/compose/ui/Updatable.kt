package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

// 可刷新的 PlatformView, 可以简化 LaunchEffect 调用

@Stable
interface Updatable<T : Any> {
    fun update(view: T)
}