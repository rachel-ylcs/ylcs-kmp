package love.yinlin.media

import org.jetbrains.skia.DirectContext
import org.jetbrains.skiko.currentDirect3DContext

internal actual fun nativeVideoGpuContext(): DirectContext? = currentDirect3DContext
