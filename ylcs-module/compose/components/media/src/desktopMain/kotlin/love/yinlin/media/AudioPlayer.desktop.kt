package love.yinlin.media

import androidx.compose.runtime.Stable
import love.yinlin.annotation.NativeLibApi
import love.yinlin.foundation.Context
import love.yinlin.platform.NativeLibLoader
import love.yinlin.platform.Platform
import love.yinlin.platform.platform

@Stable
@NativeLibApi
internal object DesktopAudioController {
    init {
        NativeLibLoader.resource("media")
    }

    fun build(context: Context, onEndListener: () -> Unit): AudioPlayer = when (platform) {
        Platform.Windows -> WindowsAudioController(context, onEndListener)
        Platform.MacOS -> MacOSAudioController(context, onEndListener)
        else -> LinuxAudioController(context, onEndListener)
    }
}

actual fun buildAudioPlayer(context: Context, onEndListener: () -> Unit): AudioPlayer = DesktopAudioController.build(context, onEndListener)