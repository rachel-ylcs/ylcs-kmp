package love.yinlin.foundation

import platform.UIKit.UIViewController

actual interface PlatformContextProvider {
    actual val raw: PlatformContext
    val controller: UIViewController?
}