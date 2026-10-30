package love.yinlin.media

import androidx.compose.runtime.Stable
import love.yinlin.foundation.LibLoader
import love.yinlin.foundation.NativeLibApi
import love.yinlin.foundation.PlatformContext
import love.yinlin.platform.Platform
import love.yinlin.platform.platform

@Stable
@NativeLibApi
internal object DesktopAudioController {
    init {
        LibLoader.resource("ylcs-module.compose.components.media")
    }

    fun build(context: PlatformContext, onEndListener: () -> Unit): AudioPlayer = when (platform) {
        Platform.Windows -> WindowsAudioController(context, onEndListener)
        else -> MiniaudioAudioController(context, onEndListener)
    }
}

actual fun buildAudioPlayer(context: PlatformContext, onEndListener: () -> Unit): AudioPlayer = DesktopAudioController.build(context, onEndListener)