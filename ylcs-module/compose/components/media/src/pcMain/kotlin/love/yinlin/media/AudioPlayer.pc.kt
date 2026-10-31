package love.yinlin.media

import love.yinlin.foundation.PlatformContext

actual fun buildAudioPlayer(context: PlatformContext, onEndListener: () -> Unit): AudioPlayer = PcAudioPlayer(context, onEndListener)
