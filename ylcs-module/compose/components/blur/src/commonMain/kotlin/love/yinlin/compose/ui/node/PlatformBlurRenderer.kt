package love.yinlin.compose.ui.node

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import kotlinx.coroutines.DisposableHandle

internal expect fun createBlurRenderer(node: CompositionLocalConsumerModifierNode): BlurRenderer

internal expect fun DrawScope.supportsBlur(): Boolean

internal expect fun needsBlurInvalidationWorkaround(): Boolean

internal expect fun registerBlurMemoryCallback(node: CompositionLocalConsumerModifierNode, onTrim: () -> Unit): DisposableHandle