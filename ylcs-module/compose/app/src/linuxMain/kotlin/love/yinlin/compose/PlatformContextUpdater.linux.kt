package love.yinlin.compose

import love.yinlin.foundation.PlatformContextProvider
import org.jetbrains.skiko.window.NativeWindowScope

actual fun PlatformContextProvider.openWindowContext(window: NativeWindowScope) {
    windowHandle = window.windowHandle
    displayHandle = window.displayHandle
}

actual fun PlatformContextProvider.closeWindowContext() {
    windowHandle = null
    displayHandle = null
}
