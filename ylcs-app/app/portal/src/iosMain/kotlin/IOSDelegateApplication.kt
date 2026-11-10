import love.yinlin.RachelApplication
import love.yinlin.compose.screen.DeepLink
import love.yinlin.foundation.PlatformContext
import love.yinlin.uri.toUri
import platform.Foundation.NSURL

class IOSDelegateApplication : RachelApplication(PlatformContext.Instance) {
    @Suppress("unused")
    fun onIOSDeepLink(uri: NSURL) = DeepLink.navigate(uri.toUri())
}
