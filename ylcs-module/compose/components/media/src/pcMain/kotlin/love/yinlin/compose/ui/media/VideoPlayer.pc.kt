package love.yinlin.compose.ui.media

import love.yinlin.foundation.PlatformContext
import love.yinlin.media.PCVideoController

actual fun buildVideoController(
    context: PlatformContext,
    topBar: VideoActionBar.Factory,
    bottomBar: VideoActionBar.Factory
): VideoController = PCVideoController(topBar, bottomBar)