package love.yinlin

import androidx.compose.ui.window.ComposeUIViewController
import love.yinlin.common.toUri
import love.yinlin.platform.ActualAppContext
import love.yinlin.platform.app
import platform.Foundation.NSURL
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val context = ActualAppContext()
    app = context
    context.initialize()
    return ComposeUIViewController {
        AppWrapper {
            App()
        }
    }
}

fun onOpenURL(url: NSURL) {
    app.model?.deeplink?.process(url.toUri())
}