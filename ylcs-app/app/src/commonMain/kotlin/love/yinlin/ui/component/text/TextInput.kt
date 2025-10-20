package love.yinlin.ui.component.text

import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.serialization.Serializable
import love.yinlin.compose.mutableRefStateOf
import love.yinlin.ui.component.image.ClickIcon

@Stable
@Serializable
enum class InputType {
    COMMON,
    PASSWORD
}

@Stable
class TextInputState(str: String = "") {
    var value: TextFieldValue by mutableRefStateOf(TextFieldValue(str))
    var overflow: Boolean by mutableStateOf(false)

    var text: String get() = value.text
        set(value) { this.value = TextFieldValue(value) }

    val ok: Boolean by derivedStateOf { !overflow && text.isNotEmpty() }

    fun insert(str: String) {
        val selection = value.selection
        value = value.copy(
            text = value.text.replaceRange(selection.start, selection.end, str),
            selection = TextRange(selection.start + str.length)
        )
    }
}

@Composable
fun rememberTextInputState(vararg keys: Any?) = remember(*keys) { TextInputState() }

@Composable
fun TextInput(
    state: TextInputState,
    hint: String? = null,
    inputType: InputType = InputType.COMMON,
    readOnly: Boolean = false,
    maxLength: Int = 0,
    maxLines: Int = 1,
    minLines: Int = maxLines,
    clearButton: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = if (clearButton) {
        {
            ClickIcon(
                icon = Icons.Outlined.Clear,
                onClick = {
                    state.text = ""
                    state.overflow = false
                }
            )
        }
    } else null,
    imeAction: ImeAction = ImeAction.Done,
    onImeClick: (KeyboardActionScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = state.value,
        onValueChange = {
            state.value = it
            state.overflow = maxLength > 0 && it.text.length > maxLength
        },
        label = hint?.let { label -> {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium
            )
        } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        readOnly = readOnly,
        visualTransformation = remember(inputType) {
            when (inputType) {
                InputType.COMMON -> VisualTransformation.None
                InputType.PASSWORD -> PasswordVisualTransformation()
            }
        },
        keyboardOptions = remember(inputType, imeAction) {
            KeyboardOptions(
                keyboardType = when (inputType) {
                    InputType.COMMON -> KeyboardType.Text
                    InputType.PASSWORD -> KeyboardType.Password
                },
                autoCorrectEnabled = false,
                imeAction = imeAction
            )
        },
        keyboardActions = remember(imeAction, onImeClick) {
            KeyboardActions(
                onDone = if (imeAction == ImeAction.Done && onImeClick != null) onImeClick else null,
                onGo = if (imeAction == ImeAction.Go && onImeClick != null) onImeClick else null,
                onNext = if (imeAction == ImeAction.Next && onImeClick != null) onImeClick else null,
                onPrevious = if (imeAction == ImeAction.Previous && onImeClick != null) onImeClick else null,
                onSearch = if (imeAction == ImeAction.Search && onImeClick != null) onImeClick else null,
                onSend = if (imeAction == ImeAction.Send && onImeClick != null) onImeClick else null,
            )
        },
        singleLine = maxLines.coerceAtLeast(1) == 1,
        minLines = minLines.coerceAtLeast(1),
        maxLines = maxLines.coerceAtLeast(1),
        isError = state.overflow,
        modifier = modifier
    )
}