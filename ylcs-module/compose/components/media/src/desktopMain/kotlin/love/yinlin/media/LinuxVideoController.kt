package love.yinlin.media

import androidx.compose.runtime.Stable
import love.yinlin.annotation.NativeLibApi
import love.yinlin.compose.ui.media.DesktopVideoController
import love.yinlin.compose.ui.media.VideoActionBar

@Stable
@NativeLibApi
internal class LinuxVideoController(topBar: VideoActionBar.Factory, bottomBar: VideoActionBar.Factory) : DesktopVideoController(topBar, bottomBar) {
    override fun nativeCreate(): Long = 0L
    override fun nativeRelease(handle: Long) { }
    override fun load(path: String) { }
    override fun play() { }
    override fun pause() { }
    override fun stop() { }
    override fun seek(position: Long) { }
}