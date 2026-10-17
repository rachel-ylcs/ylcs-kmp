package love.yinlin.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIStatusBarAnimation
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController

internal class CMPUIViewController(
    private val onViewLoad: () -> Unit,
    composeUI: @Composable () -> Unit
) : UIViewController(nibName = null, bundle = null) {
    private val composeUIController = ComposeUIViewController(composeUI)

    override fun viewDidLoad() {
        super.viewDidLoad()

        addChildViewController(composeUIController)

        val childView = composeUIController.view
        childView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(childView)

        NSLayoutConstraint.activateConstraints([
            childView.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor),
            childView.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor),
            childView.topAnchor.constraintEqualToAnchor(view.topAnchor),
            childView.bottomAnchor.constraintEqualToAnchor(view.bottomAnchor),
        ])

        composeUIController.didMoveToParentViewController(this)

        onViewLoad()
    }

    override fun preferredStatusBarStyle(): UIStatusBarStyle = composeUIController.preferredStatusBarStyle()
    override fun preferredStatusBarUpdateAnimation(): UIStatusBarAnimation = composeUIController.preferredStatusBarUpdateAnimation()
    override fun prefersStatusBarHidden(): Boolean = composeUIController.prefersStatusBarHidden()
}