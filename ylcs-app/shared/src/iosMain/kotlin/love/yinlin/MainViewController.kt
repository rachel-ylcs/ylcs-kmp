package love.yinlin

import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import love.yinlin.compose.screen.DeepLink
import love.yinlin.foundation.PlatformContextDelegate
import love.yinlin.uri.toUri
import platform.Foundation.NSURL

fun MainViewController() = object : RachelApplication(PlatformContextDelegate()) {
    override fun buildDelegate(): ComposeUIViewControllerDelegate = object : ComposeUIViewControllerDelegate {
        override fun viewDidAppear(animated: Boolean) {
            // TODO: 在 Main 启动协程
            // app.mp.floatingLyrics.initDelay(app.context)
        }
    }
}.run()

fun onOpenUri(uri: NSURL) = DeepLink.openUri(uri.toUri())