package love.yinlin

import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import love.yinlin.platform.Coroutines

fun MainViewController() = object : RachelApplication(PlatformContextDelegate) {
    override fun buildDelegate(): ComposeUIViewControllerDelegate = object : ComposeUIViewControllerDelegate {
        override fun viewDidAppear(animated: Boolean) {
            Coroutines.startMain {
                app.mp.floatingLyrics.initDelay(app.context)
            }
        }
    }
}.run()