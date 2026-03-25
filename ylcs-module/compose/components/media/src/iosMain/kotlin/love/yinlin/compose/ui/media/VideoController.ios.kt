package love.yinlin.compose.ui.media

import androidx.compose.runtime.Stable

@Stable
actual abstract class VideoController(topBar: VideoActionBar.Factory, bottomBar: VideoActionBar.Factory) : VideoState(topBar, bottomBar) {
    actual override fun release() { }
}

internal class IOSVideoController(topBar: VideoActionBar.Factory, bottomBar: VideoActionBar.Factory) : VideoController(topBar, bottomBar) {
    override fun load(path: String) { }
    override fun play() { }
    override fun pause() { }
    override fun stop() { }
    override fun seek(position: Long) { }
}