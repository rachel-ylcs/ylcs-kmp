package love.yinlin.foundation

actual open class NativeContextProvider actual constructor(actual val rawContext: PlatformContext) : NativePlatformContextProvider(), NativeDisplayContextProvider {
    /** 与窗口对应的 X11 Display 或 wl_display 指针值 */
    override var displayHandle: Long? = null
}
