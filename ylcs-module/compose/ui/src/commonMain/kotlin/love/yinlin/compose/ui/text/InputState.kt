package love.yinlin.compose.ui.text

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation

/**
 * @param initText 初始文本
 * @param maxLength 最大长度
 */
@Stable
class InputState(initText: String = "", val maxLength: Int = Int.MAX_VALUE) {
    internal val interactionSource = MutableInteractionSource()

    internal var value by mutableStateOf(initText.safe.initValue)
        private set

    internal var keyboardOptions: KeyboardOptions? by mutableStateOf(null)
    internal var keyboardActions: KeyboardActions? by mutableStateOf(null)
    internal var visualTransformation: VisualTransformation? by mutableStateOf(null)

    private inline val String.safe: String get() = if (this.length > maxLength) this.substring(0, maxLength) else this
    private inline val String.initValue: TextFieldValue get() = TextFieldValue(text = this, selection = TextRange(this.length))

    internal fun update(newValue: TextFieldValue) {
        val newText = newValue.text
        if (newText.length <= maxLength) {
            if (newText != value.text) value = newValue.copy(text = newText)
            else if (newValue.selection != value.selection || newValue.composition != value.composition) value = newValue
        }
    }

    /**
     * 设置输入框的文本内容
     */
    var text: String get() = value.text
        set(newText) {
            val safeText = newText.safe
            if (value.text != safeText) value = safeText.initValue
        }

    /**
     * 是否内容为空
     */
    val isEmpty: Boolean get() = value.text.isEmpty()

    /**
     * 是否达到最大长度
     */
    val isFull: Boolean get() = value.text.length == maxLength

    /**
     * 是否有内容且未超出最大长度
     */
    val isSafe: Boolean get() = value.text.length in 1 .. maxLength

    /**
     * 在当前光标位置插入文本
     */
    fun insert(newText: String) {
        if (newText.isNotEmpty()) {
            val selection = value.selection
            value = value.copy(
                text = value.text.replaceRange(selection.start, selection.end, newText),
                selection = TextRange(selection.start + newText.length),
                composition = null
            )
        }
    }

    /**
     * 清除输入状态
     */
    fun clear() { value = TextFieldValue() }

    override fun toString(): String = value.text
}

/**
 * @param initText 初始文本
 * @param maxLength 最大长度
 */
@Composable
fun rememberInputState(initText: String = "", maxLength: Int = Int.MAX_VALUE) = remember(initText, maxLength) { InputState(initText, maxLength) }