package love.yinlin.media

import androidx.compose.runtime.Stable
import love.yinlin.annotation.NativeLibApi
import love.yinlin.foundation.PlatformContext

@Stable
@NativeLibApi
internal class LinuxAudioController(context: PlatformContext, onEndListener: () -> Unit) :
    MiniaudioAudioController(context, onEndListener)
