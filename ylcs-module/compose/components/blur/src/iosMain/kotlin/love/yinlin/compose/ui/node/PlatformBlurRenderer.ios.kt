package love.yinlin.compose.ui.node

import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import kotlinx.coroutines.DisposableHandle
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidReceiveMemoryWarningNotification

internal actual fun registerBlurMemoryCallback(node: CompositionLocalConsumerModifierNode, onTrim: () -> Unit): DisposableHandle {
    val center = NSNotificationCenter.defaultCenter
    val observer = center.addObserverForName(
        UIApplicationDidReceiveMemoryWarningNotification,
        null,
        NSOperationQueue.mainQueue,
    ) { onTrim() }
    return DisposableHandle { center.removeObserver(observer) }
}
