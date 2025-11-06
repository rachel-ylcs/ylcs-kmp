package love.yinlin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import androidx.compose.ui.window.ComposeUIViewController
import love.yinlin.extension.LazyReference
import platform.UIKit.UIViewController

@Stable
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: LazyReference<A>,
    delegate: PlatformContextDelegate,
) : Application<A>(self, delegate) {
    @Composable
    protected open fun BeginContent() {}

    protected open fun buildDelegate(uiViewController: UIViewController): ComposeUIViewControllerDelegate = object : ComposeUIViewControllerDelegate {}

    fun run(): UIViewController {
        openService(later = false, immediate = true)

        val uiViewController = ComposeUIViewController(
            configure = {
                delegate = buildDelegate(context.uiViewController)
            }
        ) {
            Layout {
                BeginContent()
                Content()
            }
        }
        context.uiViewController = uiViewController

        return uiViewController
    }
}