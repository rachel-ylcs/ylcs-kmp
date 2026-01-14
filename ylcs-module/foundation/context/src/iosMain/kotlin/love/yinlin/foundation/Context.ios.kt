package love.yinlin.foundation

import platform.UIKit.UIViewController

actual class Context actual constructor(delegate: PlatformContextDelegate) {
    // 在 init 后可用
    lateinit var controller: UIViewController
}