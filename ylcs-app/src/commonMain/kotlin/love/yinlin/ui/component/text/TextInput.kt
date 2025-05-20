package love.yinlin.ui.component.text

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.serialization.Serializable
import love.yinlin.ui.component.image.ClickIcon

@Stable
@Serializable
enum class InputType {
	COMMON,
	PASSWORD;

	internal val toVisualTransformation: VisualTransformation get() = when (this) {
		COMMON -> VisualTransformation.None
		PASSWORD -> PasswordVisualTransformation()
	}

	internal val toKeyboardOptions: KeyboardOptions get() = when (this) {
		COMMON -> KeyboardOptions.Default
		PASSWORD -> KeyboardOptions(keyboardType = KeyboardType.Password)
	}
}

@Stable
class TextInputState(str: String = "") {
	var value: TextFieldValue by mutableStateOf(TextFieldValue(str))
	var overflow: Boolean by mutableStateOf(false)

	var text: String get() = value.text
		set(value) { this.value = TextFieldValue(value) }

	val ok: Boolean by derivedStateOf { !overflow && text.isNotEmpty() }
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
		trailingIcon = if (clearButton) {
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
		readOnly = readOnly,
		visualTransformation = remember(inputType) { inputType.toVisualTransformation },
		keyboardOptions = remember(inputType) { inputType.toKeyboardOptions },
		singleLine = maxLines.coerceAtLeast(1) == 1,
		minLines = minLines.coerceAtLeast(1),
		maxLines = maxLines.coerceAtLeast(1),
		isError = state.overflow,
		modifier = modifier
	)
}