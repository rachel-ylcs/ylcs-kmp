package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

/**
 * 可清理的 PlatformView, 可以用于释放资源
 */
@Stable
interface Releasable<T : Any> {
    fun release(view: T)
}