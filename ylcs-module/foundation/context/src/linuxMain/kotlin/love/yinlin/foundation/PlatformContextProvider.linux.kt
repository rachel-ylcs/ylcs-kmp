package love.yinlin.foundation

actual open class PlatformContextProvider actual constructor(actual val rawContext: PlatformContext) {
    /** 借用的 X11 Window ID 或 Wayland wl_surface 指针值 */
    var windowHandle: Long? = null

    /** 与窗口对应的 X11 Display 或 wl_display 指针值 */
    var displayHandle: Long? = null
}
