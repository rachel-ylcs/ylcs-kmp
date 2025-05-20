package love.yinlin.ui.component.text

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue

@Stable
class RichTextInputState(data: String = "") {
    var value: TextFieldValue by mutableStateOf(TextFieldValue())

    companion object {
        private fun parse(data: String) {

        }
    }
}

@Composable
fun RichEditor(
    state: RichTextInputState,
    title: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = state.value,
        onValueChange = { state.value = it },
        label = { Text(text = title) },
        modifier = modifier
    )
}