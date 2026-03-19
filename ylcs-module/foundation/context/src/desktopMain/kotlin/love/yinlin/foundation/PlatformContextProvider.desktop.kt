package love.yinlin.foundation

actual interface PlatformContextProvider {
    actual val raw: PlatformContext
    val windowHandle: Long?
}