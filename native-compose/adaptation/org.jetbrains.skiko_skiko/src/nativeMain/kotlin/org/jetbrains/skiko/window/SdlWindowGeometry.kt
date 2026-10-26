package org.jetbrains.skiko.window

import kotlinx.cinterop.*
import org.jetbrains.skiko.internal.sdl.*

internal class SdlWindowGeometry(val window: COpaquePointer) {
    val isShowing: Boolean get() = RachelSDL_IsWindowShowing(window)
    val width: Int get() = memScoped {
        val size = alloc<IntVar>()
        check(RachelSDL_GetWindowSizeInPixels(window, size.ptr, null))
        size.value
    }
    val height: Int get() = memScoped {
        val size = alloc<IntVar>()
        check(RachelSDL_GetWindowSizeInPixels(window, null, size.ptr))
        size.value
    }
    val scale: Float get() = RachelSDL_GetWindowDisplayScale(window)
    var fullscreen: Boolean
        get() = RachelSDL_IsFullscreen(window)
        set(value) { check(RachelSDL_SetFullscreen(window, value)) }
}
