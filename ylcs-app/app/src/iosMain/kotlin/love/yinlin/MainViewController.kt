package love.yinlin

import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.delay
import love.yinlin.platform.ActualFloatingLyrics
import love.yinlin.platform.Coroutines
import love.yinlin.service.PlatformContext
import platform.UIKit.UIViewController

lateinit var controller: UIViewController

fun MainViewController(): UIViewController {
    service.init(PlatformContext)

    return ComposeUIViewController(
        configure = {
            delegate = object : ComposeUIViewControllerDelegate {
                override fun viewDidAppear(animated: Boolean) {
                    ActualFloatingLyrics(controller).let {
                        service.musicFactory.instance.floatingLyrics = it
                        if (service.config.enabledFloatingLyrics) {
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
        AppEntry {
            ScreenEntry()
        }
    }.apply {
        controller = this
    }
}