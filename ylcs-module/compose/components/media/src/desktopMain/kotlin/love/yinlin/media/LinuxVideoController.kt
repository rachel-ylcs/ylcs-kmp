package love.yinlin.media

import androidx.compose.runtime.Stable
import love.yinlin.annotation.NativeLibApi
import love.yinlin.compose.ui.media.VideoActionBar
import love.yinlin.compose.ui.media.VideoController
import love.yinlin.foundation.Context

@Stable
@NativeLibApi
internal class LinuxVideoController(context: Context, topBar: VideoActionBar?, bottomBar: VideoActionBar?) : VideoController(context, topBar, bottomBar) {
    override fun nativeCreate(): Long = 0L
    override fun nativeRelease(handle: Long) { }
    override fun load(path: String) { }
    override fun play() { }
    override fun pause() { }
    override fun stop() { }
    override fun seek(position: Long) { }
}