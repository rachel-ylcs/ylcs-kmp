import love.yinlin.RachelApplication
import love.yinlin.foundation.PlatformContext

fun main() = object : RachelApplication(PlatformContext.Instance) {
    init { sync(block = ::listenForWebLaunches) }
}.run()
