package love.yinlin.compose.ui.input

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.ImeAction

/**
 * 数字输入器的状态。仅允许保存 0-9，最多 8 位；空内容的 [number] 为 0。
 *
 * [value] 保留前导零，[number] 将其转换为整数。外部设置 [value] 可用于重置输入，
 * 用户在 [NumberInput] 内只能在末尾追加或删除一位。
 */
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

/** 记住数字输入状态；[initialValue] 仅在创建或参数变化时用于初始化。 */
@Composable
fun rememberNumberInputState(
    initialValue: String = "",
    imeAction: ImeAction = ImeAction.Done,
): NumberInputState = remember(initialValue, imeAction) {
    NumberInputState(initialValue, imeAction)
}
