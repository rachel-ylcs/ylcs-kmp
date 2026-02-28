package love.yinlin.compose.ui.text

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable

@Composable
@NonRestartableComposable
fun SelectionBox(
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    if (enabled) SelectionContainer(content = content)
    else content()
}