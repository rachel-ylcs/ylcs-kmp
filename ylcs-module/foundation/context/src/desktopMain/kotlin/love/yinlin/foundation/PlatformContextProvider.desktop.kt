package love.yinlin.foundation

actual open class PlatformContextProvider actual constructor(actual val rawContext: PlatformContext) {
    var windowHandle: Long? = null
        protected set
}