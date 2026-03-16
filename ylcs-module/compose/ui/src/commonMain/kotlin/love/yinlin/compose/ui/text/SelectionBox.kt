package love.yinlin.compose.ui.text

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import love.yinlin.compose.extension.rememberMovableContent

@Composable
@NonRestartableComposable
fun SelectionBox(enabled: Boolean = true, content: @Composable () -> Unit) {
    val movableContent = rememberMovableContent(content)
    if (enabled) SelectionContainer(content = movableContent)
    else movableContent()
}