package love.yinlin.foundation

actual interface PlatformContextProvider {
    actual val raw: PlatformContext

    companion object {
        val Instance = object : PlatformContextProvider {
            override val raw: PlatformContext = PlatformContext.Instance
        }
    }
}