package love.yinlin.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import love.yinlin.app
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.media.VideoActionBar
import love.yinlin.compose.ui.media.VideoPlayer
import love.yinlin.compose.ui.media.buildVideoController

@Stable
class ScreenVideo(val url: String) : Screen() {
    override val title: String? = null

    val controller = buildVideoController(app.context, viewModelScope, app.config.audioFocus, VideoActionBar.topDefault(::onBack))

    override suspend fun initialize() {
        controller.load(url)
    }

    override fun finalize() {
        controller.release()
    }

    @Composable
    override fun Content() {
        VideoPlayer(
            controller = controller,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        )
    }
}