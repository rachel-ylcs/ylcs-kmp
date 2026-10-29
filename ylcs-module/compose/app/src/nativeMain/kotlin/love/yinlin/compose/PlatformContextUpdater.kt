package love.yinlin.compose

import love.yinlin.foundation.PlatformContextProvider
import org.jetbrains.skiko.window.NativeWindowScope

expect fun PlatformContextProvider.openWindowContext(window: NativeWindowScope)
expect fun PlatformContextProvider.closeWindowContext()
