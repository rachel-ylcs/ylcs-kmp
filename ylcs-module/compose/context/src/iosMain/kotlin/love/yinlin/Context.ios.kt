package love.yinlin

import androidx.compose.runtime.Stable
import platform.UIKit.UIViewController

@Stable
actual class Context actual constructor(delegate: PlatformContextDelegate) {
    // 在 init 后可用
    lateinit var uiViewController: UIViewController
}