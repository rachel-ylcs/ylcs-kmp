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
import androidx.compose.ui.text.input.VisualTransformation
import love.yinlin.ui.component.image.ClickIcon

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

class TextInputState(str: String = "", isOverflow: Boolean = false) {
	var text: String by mutableStateOf(str)
	var overflow: Boolean by mutableStateOf(isOverflow)

	val ok: Boolean by derivedStateOf { !overflow && text.isNotEmpty() }
}

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
					imageVector = Icons.Outlined.Clear,
					onClick = {
						state.text = ""
						state.overflow = false
					}
				)
			}
		} else null,
		visualTransformation = remember(inputType) { inputType.toVisualTransformation },
		keyboardOptions = remember(inputType) { inputType.toKeyboardOptions },
		singleLine = maxLines.coerceAtLeast(1) == 1,
		maxLines = maxLines.coerceAtLeast(1),
		isError = state.overflow,
		modifier = modifier
	)
}