package love.yinlin.compose.ui.text

import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction

@Stable
enum class PasswordShowMode {
    Hidden, // 完全隐藏
    LastShow; // 显示最后一个

    internal val asTextObfuscationMode: TextObfuscationMode get() = when (this) {
        Hidden -> TextObfuscationMode.Hidden
        LastShow -> TextObfuscationMode.RevealLastTyped
    }
}

@Stable
data class PasswordMask(
    val character: Char = '\u2022',
    val mode: PasswordShowMode = PasswordShowMode.Hidden
)

@Stable
class PasswordInputState(
    initText: String = "",
    maxLength: Int = Int.MAX_VALUE,
    imeAction: ImeAction = ImeAction.Done,
    internal val mask: PasswordMask = PasswordMask()
) : InputState(initText, maxLength, 1, 1, imeAction) {
    internal var maskStatus by mutableStateOf(mask.mode.asTextObfuscationMode)
}

/**
 * @param initText 初始文本
 * @param maxLength 最大长度
 * @param imeAction IME按钮类型
 */
@Composable
fun rememberPasswordInputState(
    initText: String = "",
    maxLength: Int = Int.MAX_VALUE,
    imeAction: ImeAction = ImeAction.Done
) = remember(initText, maxLength, imeAction) {
    PasswordInputState(initText, maxLength, imeAction)
}