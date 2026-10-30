package love.yinlin.compose

import love.yinlin.foundation.PlatformContextProvider
import love.yinlin.foundation.NativeDisplayContextProvider
import org.jetbrains.skiko.window.NativeWindowScope

fun PlatformContextProvider.openWindowContext(window: NativeWindowScope) {
    windowHandle = window.windowHandle
    (this as? NativeDisplayContextProvider)?.displayHandle = window.displayHandle
}

fun PlatformContextProvider.closeWindowContext() {
    windowHandle = null
    (this as? NativeDisplayContextProvider)?.displayHandle = null
}
