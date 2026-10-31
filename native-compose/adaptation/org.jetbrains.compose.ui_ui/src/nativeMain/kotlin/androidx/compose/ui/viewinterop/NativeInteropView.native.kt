package androidx.compose.ui.viewinterop

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect

@ExperimentalComposeUiApi
interface NativeInteropView : AutoCloseable {
    val nativeHandle: Long
    val hasFocus: Boolean
    fun place(bounds: Rect, visibleBounds: Rect)
    fun bringToFront()
    fun hide()
}
