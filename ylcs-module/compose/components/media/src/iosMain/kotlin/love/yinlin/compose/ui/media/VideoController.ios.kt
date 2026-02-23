package love.yinlin.compose.ui.media

import androidx.compose.runtime.Stable
import love.yinlin.foundation.Context

@Stable
actual abstract class VideoController(context: Context, topBar: VideoActionBar?, bottomBar: VideoActionBar?) : VideoState(context, topBar, bottomBar) {
    actual override fun release() { }
}

internal class IOSVideoController(context: Context, topBar: VideoActionBar?, bottomBar: VideoActionBar?) : VideoController(context, topBar, bottomBar) {
    override fun load(path: String) { }
    override fun play() { }
    override fun pause() { }
    override fun stop() { }
    override fun seek(position: Long) { }
}