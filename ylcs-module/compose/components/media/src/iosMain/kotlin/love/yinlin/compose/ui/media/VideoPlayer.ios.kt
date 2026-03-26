package love.yinlin.compose.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import love.yinlin.compose.ui.tool.UnsupportedPlatformComponent
import love.yinlin.foundation.PlatformContext

@Stable
private class IOSVideoController(topBar: VideoActionBar.Factory, bottomBar: VideoActionBar.Factory) : VideoController(topBar, bottomBar) {
    override fun releaseController() { }
    override fun load(path: String) { }
    override fun play() { }
    override fun pause() { }
    override fun stop() { }
    override fun seek(position: Long) { }

    @Composable
    override fun SurfaceContent(modifier: Modifier) {
        UnsupportedPlatformComponent(modifier = modifier)
    }
}

actual fun buildVideoController(
    context: PlatformContext,
    topBar: VideoActionBar.Factory,
    bottomBar: VideoActionBar.Factory
): VideoController = IOSVideoController(topBar, bottomBar)