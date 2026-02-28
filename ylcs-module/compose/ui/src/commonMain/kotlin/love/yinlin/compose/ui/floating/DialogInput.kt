package love.yinlin.compose.ui.floating

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.compose.Theme
import love.yinlin.compose.ValueTheme
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputDecoration
import love.yinlin.compose.ui.text.InputState

@Stable
class DialogInput(
    val hint: String = "",
    maxLength: Int = Int.MAX_VALUE,
    val maxLines: Int = 1,
    val minLines: Int = maxLines,
    val leading: InputDecoration? = null,
    val trailing: InputDecoration? = null
) : DialogTemplate<String>() {
    override val icon: ImageVector = Icons.Edit
    override val scrollable: Boolean = false

    private var title: String? by mutableStateOf(ValueTheme.runtime())
    private val textInputState = InputState(maxLength = maxLength)
    private val focusRequester = FocusRequester()

    override val actions: @Composable (RowScope.() -> Unit) = {
        TextButton(text = Theme.value.dialogOkText, enabled = textInputState.isSafe, color = Theme.color.primary, onClick = { future?.send(textInputState.text) })
        TextButton(text = Theme.value.dialogCancelText, onClick = ::close)
    }

    suspend fun open(initText: String = "", title: String? = ValueTheme.runtime()): String? {
        this.title = title
        textInputState.text = initText
        return awaitResult()
    }

    @Composable
    override fun Land() {
        LandDialogTemplate(title = title ?: Theme.value.dialogInputTitle) {
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            Input(
                state = textInputState,
                hint = hint,
                maxLines = maxLines,
                minLines = minLines,
                onImeClick = {
                    if (textInputState.isSafe) future?.send(textInputState.text)
                },
                leading = leading,
                trailing = trailing,
                modifier = Modifier.widthIn(min = minContentWidth).focusRequester(focusRequester)
            )
        }
    }
}