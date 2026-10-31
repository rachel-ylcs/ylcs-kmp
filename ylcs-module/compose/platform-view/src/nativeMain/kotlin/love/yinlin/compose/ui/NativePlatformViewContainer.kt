package love.yinlin.compose.ui

import androidx.compose.ui.geometry.Rect

internal interface NativePlatformViewContainer<V : Any> : AutoCloseable {
    val nativeHandle: Long
    val hasFocus: Boolean
    fun create(): V
    fun place(bounds: Rect, visibleBounds: Rect)
    fun bringToFront()
    fun hide()
}
