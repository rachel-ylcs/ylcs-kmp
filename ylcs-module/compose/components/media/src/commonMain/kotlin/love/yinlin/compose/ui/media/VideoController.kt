package love.yinlin.compose.ui.media

import androidx.compose.runtime.Stable

@Stable
expect abstract class VideoController : VideoState {
    override fun release()
}