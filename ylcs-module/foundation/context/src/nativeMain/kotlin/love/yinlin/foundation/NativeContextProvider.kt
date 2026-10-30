package love.yinlin.foundation

expect open class NativeContextProvider(rawContext: PlatformContext) : NativePlatformContextProvider {
    val rawContext: PlatformContext
}
