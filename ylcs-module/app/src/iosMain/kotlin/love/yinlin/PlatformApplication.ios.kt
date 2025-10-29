package love.yinlin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import androidx.compose.ui.window.ComposeUIViewController
import love.yinlin.extension.Reference
import platform.UIKit.UIViewController

@Stable
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: Reference<A?>,
    delegate: PlatformContextDelegate,
) : Application<A>(self, delegate) {
    private lateinit var uiViewController: UIViewController

    @Composable
    protected open fun BeginContent() {}

    protected open fun buildDelegate(uiViewController: UIViewController): ComposeUIViewControllerDelegate = object : ComposeUIViewControllerDelegate {}

    fun run(): UIViewController {
        initialize()

        uiViewController = ComposeUIViewController(
            configure = {
                delegate = buildDelegate(uiViewController)
            }
        ) {
            BeginContent()
            Layout()
        }

        return uiViewController
    }
}