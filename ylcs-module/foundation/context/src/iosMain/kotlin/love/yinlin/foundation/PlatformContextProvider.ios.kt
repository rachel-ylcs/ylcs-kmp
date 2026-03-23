package love.yinlin.foundation

import platform.UIKit.UIViewController

actual open class PlatformContextProvider actual constructor(actual val rawContext: PlatformContext) {
    var controller: UIViewController? = null
        protected set
}