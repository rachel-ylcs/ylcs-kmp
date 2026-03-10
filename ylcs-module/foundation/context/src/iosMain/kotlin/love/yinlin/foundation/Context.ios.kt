package love.yinlin.foundation

import platform.UIKit.UIViewController

actual class Context actual constructor(val delegate: PlatformContextDelegate) {
    // 在 initLater 后可用
    lateinit var controller: UIViewController
}