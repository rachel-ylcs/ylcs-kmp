package love.yinlin.compose.ui.text

import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme

/**
 * @param state 状态
 * @param hint 提示
 * @param enabled 可写入
 * @param style 文字样式
 * @param onImeClick IME按钮点击事件
 * @param colorProvider 提示灯颜色
 * @param leading 头部装饰
 * @param trailing 尾部装饰
 */
@Composable
fun PasswordInput(
    modifier: Modifier = Modifier,
    state: PasswordInputState = rememberPasswordInputState(),
    hint: String? = null,
    enabled: Boolean = true,
    style: TextStyle = LocalStyle.current,
    alignment: Alignment.Vertical = Alignment.CenterVertically,
    onImeClick: (() -> Boolean)? = null,
    colorProvider: InputStatusColorProvider = InputStatusColorProvider.Default,
    leading: InputDecoration? = null,
    trailing: InputDecoration = InputDecoration.PasswordViewer,
) {
    val contentColor = if (enabled) Theme.color.onBackground else Theme.color.disabledContent
    val textStyle = if (style.color == Color.Unspecified) style.copy(color = contentColor) else style

    CompositionLocalProvider(
        LocalColor provides contentColor,
        LocalColorVariant provides Theme.color.onBackgroundVariant,
        LocalStyle provides textStyle
    ) {
        BasicSecureTextField(
            state = state.rawState,
            modifier = modifier,
            enabled = enabled,
            readOnly = !enabled,
            inputTransformation = state.inputTransformation,
            textStyle = textStyle,
            keyboardOptions = remember(state.imeAction) {
                KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrectEnabled = false,
                    imeAction = state.imeAction
                )
            },
            onKeyboardAction = remember(onImeClick) {
                KeyboardActionHandler { perform ->
                    if (onImeClick?.invoke() == true) perform()
                }
            },
            onTextLayout = null,
            interactionSource = state.interactionSource,
            cursorBrush = remember(contentColor) { SolidColor(contentColor) },
            decorator = { innerContent ->
                DecorationBox(state, hint, alignment, enabled, colorProvider, leading, trailing, innerContent)
            },
            textObfuscationMode = state.maskStatus,
            textObfuscationCharacter = state.mask.character,
            scrollState = state.scrollState
        )
    }
}