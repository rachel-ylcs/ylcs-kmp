package love.yinlin.compose.ui.floating

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
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
class DialogPairInput(
    val hint1: String = "",
    val hint2: String = "",
    maxLength1: Int = Int.MAX_VALUE,
    maxLength2: Int = Int.MAX_VALUE,
    val maxLines1: Int = 1,
    val maxLines2: Int = 1,
    val minLines1: Int = maxLines1,
    val minLines2: Int = maxLines2,
    val leading1: InputDecoration? = null,
    val leading2: InputDecoration? = null,
    val trailing1: InputDecoration? = null,
    val trailing2: InputDecoration? = null,
) : DialogTemplate<Pair<String, String>>() {
    override val icon: ImageVector = Icons.Edit
    override val scrollable: Boolean = false

    private var title: String? by mutableStateOf(ValueTheme.runtime())
    private val textInputState1 = InputState(maxLength = maxLength1)
    private val textInputState2 = InputState(maxLength = maxLength2)
    private val focusRequester = FocusRequester()

    private val canSubmit by derivedStateOf { textInputState1.isSafe && textInputState2.isSafe }

    override val actions: @Composable (RowScope.() -> Unit) = {
        TextButton(
            text = Theme.value.dialogOkText,
            enabled = canSubmit,
            color = Theme.color.primary,
            onClick = { future?.send(textInputState1.text to textInputState2.text) }
        )
        TextButton(text = Theme.value.dialogCancelText, onClick = ::close)
    }

    suspend fun open(initText1: String = "", initText2: String, title: String? = ValueTheme.runtime()): Pair<String, String>? {
        this.title = title
        textInputState1.text = initText1
        textInputState2.text = initText2
        return awaitResult()
    }

    @Composable
    override fun Land() {
        LandDialogTemplate(title = title ?: Theme.value.dialogInputTitle) {
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            Column(verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)) {
                Input(
                    state = textInputState1,
                    hint = hint1,
                    maxLines = maxLines1,
                    minLines = minLines1,
                    leading = leading1,
                    trailing = trailing1,
                    modifier = Modifier.widthIn(min = minContentWidth).focusRequester(focusRequester)
                )
                Input(
                    state = textInputState2,
                    hint = hint2,
                    maxLines = maxLines2,
                    minLines = minLines2,
                    leading = leading2,
                    trailing = trailing2,
                    modifier = Modifier.widthIn(min = minContentWidth)
                )
            }
        }
    }
}