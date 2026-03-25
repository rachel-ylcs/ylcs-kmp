package love.yinlin.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import love.yinlin.app
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.media.VideoActionBar
import love.yinlin.compose.ui.media.VideoPlayer
import love.yinlin.compose.ui.media.buildVideoController

@Stable
class ScreenVideo(val url: String) : BasicScreen() {
    private val controller = buildVideoController(app.rawContext, VideoActionBar.topDefault(::onBack))

    override suspend fun initialize() {
        controller.load(url)
    }

    override fun finalize() {
        controller.release()
    }

    @Composable
    override fun BasicContent() {
        VideoPlayer(
            controller = controller,
            modifier = Modifier.fillMaxSize().background(Colors.Black).padding(LocalImmersivePadding.current)
        )
    }
}