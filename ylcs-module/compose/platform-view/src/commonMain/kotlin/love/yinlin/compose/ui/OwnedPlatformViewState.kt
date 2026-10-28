package love.yinlin.compose.ui

import androidx.compose.runtime.RememberObserver

internal class OwnedPlatformViewState<S : PlatformViewStateOwner>(val state: S) : RememberObserver {
    override fun onRemembered() {}
    override fun onForgotten() = state.release()
    override fun onAbandoned() = state.release()
}
