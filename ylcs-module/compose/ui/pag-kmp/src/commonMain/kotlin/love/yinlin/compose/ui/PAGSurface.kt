package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap

@Stable
expect class PAGSurface {
    companion object {
        fun makeOffscreen(width: Int, height: Int): PAGSurface
    }

    val width: Int
    val height: Int
    fun updateSize()
    fun freeCache()
    fun clearAll()
    fun makeSnapshot(): ImageBitmap?
    fun close()
}