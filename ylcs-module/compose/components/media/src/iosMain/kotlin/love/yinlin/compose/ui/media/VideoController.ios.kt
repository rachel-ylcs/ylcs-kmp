package love.yinlin.compose.ui.media

import androidx.compose.runtime.Stable

@Stable
actual abstract class VideoController(topBar: VideoActionBar?, bottomBar: VideoActionBar?) : VideoState(topBar, bottomBar) {
    actual override fun release() { }
}

internal class IOSVideoController(topBar: VideoActionBar?, bottomBar: VideoActionBar?) : VideoController(topBar, bottomBar) {
    override fun load(path: String) { }
    override fun play() { }
    override fun pause() { }
    override fun stop() { }
    override fun seek(position: Long) { }
}