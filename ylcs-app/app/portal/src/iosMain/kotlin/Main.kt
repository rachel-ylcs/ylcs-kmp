import love.yinlin.RachelApplication
import love.yinlin.compose.screen.DeepLink
import love.yinlin.foundation.PlatformContextDelegate
import love.yinlin.uri.toUri
import platform.Foundation.NSURL

class IOSDelegateApplication : RachelApplication(PlatformContextDelegate()) {
    fun onIOSDeepLink(uri: NSURL) = DeepLink.openUri(uri.toUri())
}