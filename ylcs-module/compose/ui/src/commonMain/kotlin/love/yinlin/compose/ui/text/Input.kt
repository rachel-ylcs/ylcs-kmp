package love.yinlin.compose.ui.text

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme

/**
 * @param state 状态
 * @param hint 提示
 * @param enabled 可写入
 * @param style 文字样式
 * @param alignment 文字对齐方式
 * @param colorProvider 提示灯颜色
 * @param leading 头部装饰
 * @param trailing 尾部装饰
 */
@Composable
fun Input(
    modifier: Modifier = Modifier,
    state: InputState = rememberInputState(),
    hint: String? = null,
    enabled: Boolean = true,
    style: TextStyle = LocalStyle.current,
    alignment: Alignment.Vertical = Alignment.CenterVertically,
    onImeClick: (() -> Boolean)? = null,
    colorProvider: InputStatusColorProvider = InputStatusColorProvider.Default,
    leading: InputDecoration? = null,
    trailing: InputDecoration? = null,
) {
    val contentColor = if (enabled) Theme.color.onBackground else Theme.color.disabledContent
    val textStyle = if (style.color == Colors.Unspecified) style.copy(color = contentColor) else style

    CompositionLocalProvider(
        LocalColor provides contentColor,
        LocalColorVariant provides Theme.color.onBackgroundVariant,
        LocalStyle provides textStyle
    ) {
        BasicTextField(
            state = state.rawState,
            modifier = modifier,
            enabled = enabled,
            readOnly = !enabled,
            inputTransformation = state.inputTransformation,
            textStyle = textStyle,
            keyboardOptions = remember(state.imeAction) {
                KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    autoCorrectEnabled = false,
                    imeAction = state.imeAction
                )
            },
            onKeyboardAction = remember(onImeClick) {
                KeyboardActionHandler { perform ->
                    if (onImeClick?.invoke() == true) perform()
                }
            },
            lineLimits = state.lineLimits,
            interactionSource = state.interactionSource,
            cursorBrush = remember(contentColor) { SolidColor(contentColor) },
            outputTransformation = null,
            decorator = remember(state, hint, alignment, enabled, colorProvider, leading, trailing) {
                TextFieldDecorator { innerTextField ->
                    DecorationBox(state, hint, alignment, enabled, colorProvider, leading, trailing, innerTextField)
                }
            },
            scrollState = state.scrollState
        )
    }
}
