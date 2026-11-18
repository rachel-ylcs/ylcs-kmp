package love.yinlin.compose.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier

@Composable
fun DateInput(
    modifier: Modifier = Modifier,
    state: DateInputState,
    hint: String? = null,
) {
    DisposableEffect(state) {
        onDispose(state::resetPicker)
    }

    Input(
        modifier = modifier,
        state = state.inputState,
        hint = hint,
        readOnly = true,
        trailing = state.trailing
    )
}
