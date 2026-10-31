package love.yinlin.compose.ui.media

import love.yinlin.foundation.PlatformContext
import love.yinlin.media.PcVideoController

actual fun buildVideoController(
    context: PlatformContext,
    topBar: VideoActionBar.Factory,
    bottomBar: VideoActionBar.Factory
): VideoController = PcVideoController(topBar, bottomBar)