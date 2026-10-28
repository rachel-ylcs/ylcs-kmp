package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
interface PlatformViewStateOwner {
    val isActive: Boolean
    fun release()
}
