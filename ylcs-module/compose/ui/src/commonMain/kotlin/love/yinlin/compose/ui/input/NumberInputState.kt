package love.yinlin.compose.ui.input

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.ImeAction

@Stable
class NumberInputState(
    initialValue: String = "",
    internal val imeAction: ImeAction = ImeAction.Done,
) {
    init {
        require(initialValue.length <= 8 && initialValue.all { it in '0' .. '9' }) {
            "NumberInput initialValue must contain at most 8 digits (0-9)"
        }
    }

    internal val rawState = TextFieldState(initialValue)
    internal val interactionSource = MutableInteractionSource()

    var value: String
        get() = rawState.text.toString()
        set(newValue) {
            require(newValue.length <= 8 && newValue.all { it in '0' .. '9' }) {
                "NumberInput value must contain at most 8 digits (0-9)"
            }
            rawState.setTextAndPlaceCursorAtEnd(newValue)
        }

    val number: Int get() = value.toIntOrNull() ?: 0

    val isEmpty: Boolean get() = rawState.text.isEmpty()

    fun clear() = rawState.clearText()

    internal fun deleteLast() {
        val current = value
        if (current.isNotEmpty()) rawState.setTextAndPlaceCursorAtEnd(current.dropLast(1))
    }
}