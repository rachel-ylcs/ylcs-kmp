package androidx.compose.ui.focus

import androidx.compose.ui.node.CompositionLocalConsumerModifierNode

internal actual fun systemDefinedCanFocus(node: CompositionLocalConsumerModifierNode): Boolean = true
