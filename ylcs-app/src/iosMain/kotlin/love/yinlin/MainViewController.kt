package love.yinlin

import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.delay
import love.yinlin.common.DeepLinkHandler
import love.yinlin.common.toUri
import love.yinlin.platform.ActualAppContext
import love.yinlin.platform.ActualFloatingLyrics
import love.yinlin.platform.Coroutines
import love.yinlin.platform.app
import love.yinlin.platform.appNative
import platform.Foundation.NSURL
import platform.UIKit.UIViewController

lateinit var controller: UIViewController

fun MainViewController(): UIViewController {
    ActualAppContext().apply {
        app = this
        initialize()
    }

    return ComposeUIViewController(
        configure = {
            delegate = object : ComposeUIViewControllerDelegate {
                override fun viewDidAppear(animated: Boolean) {
                    ActualFloatingLyrics(controller).let {
                        appNative.musicFactory.floatingLyrics = it
                        if (appNative.config.enabledFloatingLyrics) {
                            Coroutines.startMain {
                                delay(1000L)
                                it.attach()
                            }
                        }
                    }
                }
            }
        }
    ) {
        AppWrapper {
            App()
        }
    }.apply {
        controller = this
    }
}

fun onOpenURL(url: NSURL) {
    DeepLinkHandler.onOpenUri(url.toUri())
}