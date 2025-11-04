package love.yinlin

import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import kotlinx.coroutines.delay
import love.yinlin.platform.Coroutines
import platform.UIKit.UIViewController

fun MainViewController() = object : RachelApplication(PlatformContextDelegate) {
    override fun buildDelegate(uiViewController: UIViewController): ComposeUIViewControllerDelegate = object : ComposeUIViewControllerDelegate {
        override fun viewDidAppear(animated: Boolean) {
            if (config.enabledFloatingLyrics) {
                Coroutines.startMain {
                    delay(1000L)
                    app.mp.floatingLyrics.attach()
                }
            }
        }
    }
}.run()