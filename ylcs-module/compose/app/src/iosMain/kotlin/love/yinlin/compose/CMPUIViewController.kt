package love.yinlin.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIStatusBarAnimation
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController

internal abstract class CMPUIViewController : UIViewController(nibName = null, bundle = null) {
    @Composable
    protected abstract fun ComposeUI()

    protected abstract fun onViewLoad()

    private val composeUIController = ComposeUIViewController(::ComposeUI)

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

    final override fun preferredStatusBarStyle(): UIStatusBarStyle = composeUIController.preferredStatusBarStyle()

    final override fun preferredStatusBarUpdateAnimation(): UIStatusBarAnimation = composeUIController.preferredStatusBarUpdateAnimation()

    final override fun prefersStatusBarHidden(): Boolean = composeUIController.prefersStatusBarHidden()
}