package love.yinlin.ui.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

enum class InputType {
	COMMON,
	PASSWORD
}

private fun InputType.toVisualTransformation() = when (this) {
	InputType.COMMON -> VisualTransformation.None
	InputType.PASSWORD -> PasswordVisualTransformation()
}

private fun InputType.toKeyboardOptions() = when (this) {
	InputType.COMMON -> KeyboardOptions.Default
	InputType.PASSWORD -> KeyboardOptions(keyboardType = KeyboardType.Password)
}

class TextInputState {
	var text: String by mutableStateOf("")
	var overflow: Boolean by mutableStateOf(false)

	companion object {
		val saver = listSaver(
			save = { listOf(it.text, it.overflow) },
			restore = {
				TextInputState().apply {
					text = it[0] as String
					overflow = it[1] as Boolean
				}
			}
		)
	}
}

@Composable
fun rememberTextInputState() = rememberSaveable(saver = TextInputState.saver) { TextInputState() }

@Composable
fun TextInput(
	state: TextInputState,
	hint: String,
	inputType: InputType = InputType.COMMON,
	maxLength: Int = 0,
	maxLines: Int = 1,
	clearButton: Boolean = true,
	modifier: Modifier = Modifier
) {
	OutlinedTextField(
		value = state.text,
		onValueChange = {
			state.text = it
			state.overflow = maxLength > 0 && it.length > maxLength
		},
		label = {
			Text(
				text = hint,
				style = MaterialTheme.typography.titleMedium
			)
		},
		trailingIcon = if (clearButton) {
			{
				ClickIcon(
					imageVector = Icons.Filled.Clear,
					onClick = {
						state.text = ""
					}
				)
			}
		} else null,
		visualTransformation = remember(inputType) { inputType.toVisualTransformation() },
		keyboardOptions = remember(inputType) { inputType.toKeyboardOptions() },
		singleLine = maxLines.coerceAtLeast(1) == 1,
		maxLines = maxLines.coerceAtLeast(1),
		isError = state.overflow,
		modifier = modifier
	)
}