package love.yinlin.compose.ui.node

import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import kotlinx.coroutines.DisposableHandle

internal actual fun registerBlurMemoryCallback(node: CompositionLocalConsumerModifierNode, onTrim: () -> Unit): DisposableHandle = DisposableHandle { }
