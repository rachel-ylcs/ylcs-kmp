package love.yinlin.compose.ui.text

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.maxLengthTrim
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceIn

/**
 * @param initText 初始文本
 * @param maxLength 最大长度
 * @param maxLines 最大行
 * @param minLines 最小行
 * @param imeAction IME按钮类型
 */
@Stable
open class InputState(
    initText: String = "",
    internal val maxLength: Int = Int.MAX_VALUE,
    maxLines: Int = 1,
    minLines: Int = maxLines,
    internal val imeAction: ImeAction = ImeAction.Done
) {
    internal val interactionSource = MutableInteractionSource()

    internal val rawState = TextFieldState(initText)

    internal val inputTransformation: InputTransformation = InputTransformation.maxLengthTrim(maxLength)

    internal val lineLimits = if (maxLines == 1) TextFieldLineLimits.SingleLine else {
        val max = maxLines.fastCoerceAtLeast(1)
        val min = minLines.fastCoerceIn(1, max)
        TextFieldLineLimits.MultiLine(min, max)
    }

    internal val scrollState: ScrollState = ScrollState(0)

    /**
     * 设置输入框的文本内容
     */
    var text: String get() = rawState.text.toString()
        set(newText) { rawState.setTextAndPlaceCursorAtEnd(newText) }

    /**
     * 是否内容为空
     */
    val isEmpty: Boolean get() = rawState.text.isEmpty()

    /**
     * 是否内容不为空
     */
    val isNotEmpty: Boolean get() = rawState.text.isNotEmpty()

    /**
     * 是否达到最大长度
     */
    val isFull: Boolean get() = rawState.text.length == maxLength

    /**
     * 是否有内容且未超出最大长度
     */
    val isSafe: Boolean get() = rawState.text.length in 1 .. maxLength

    /**
     * 在当前光标位置插入文本
     */
    fun insert(newText: String) {
        if (newText.isNotEmpty()) {
            rawState.edit {
                val oldSelection = selection
                replace(oldSelection.start, oldSelection.end, newText)
                selection = TextRange(oldSelection.start + newText.length)
            }
        }
    }

    /**
     * 清除输入状态
     */
    fun clear() = rawState.clearText()

    override fun toString(): String = rawState.text.toString()
}

/**
 * @param initText 初始文本
 * @param maxLength 最大长度
 * @param maxLines 最大行
 * @param minLines 最小行
 * @param imeAction IME按钮类型
 */
@Composable
fun rememberInputState(
    initText: String = "",
    maxLength: Int = Int.MAX_VALUE,
    maxLines: Int = 1,
    minLines: Int = maxLines,
    imeAction: ImeAction = ImeAction.Done
) = remember(initText, maxLength, maxLines, minLines, imeAction) {
    InputState(initText, maxLength, maxLines, minLines, imeAction)
}