package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

/**
 * 可重置的 PlatformView, 可以用于 ListView 下共享复用 Host 实例
 */
@Stable
interface Resettable<T : Any> {
    fun reset(view: T)
}