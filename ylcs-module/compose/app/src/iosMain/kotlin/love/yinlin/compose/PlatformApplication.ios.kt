package love.yinlin.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import androidx.compose.ui.window.ComposeUIViewController
import love.yinlin.extension.BaseLazyReference
import love.yinlin.foundation.PlatformContextDelegate
import platform.UIKit.UIViewController

@Stable
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: BaseLazyReference<A>,
    delegate: PlatformContextDelegate,
) : Application<A>(self, delegate) {
    @Composable
    protected open fun BeginContent() {}

    protected open fun buildDelegate(): ComposeUIViewControllerDelegate = object : ComposeUIViewControllerDelegate {}

    fun run(): UIViewController {
        openService(later = false, immediate = true)

        val uiViewController = ComposeUIViewController(
            configure = {
                delegate = buildDelegate()
            }
        ) {
            Layout {
                BeginContent()
                Content()
            }
        }
        context.controller = uiViewController

        return uiViewController
    }
}