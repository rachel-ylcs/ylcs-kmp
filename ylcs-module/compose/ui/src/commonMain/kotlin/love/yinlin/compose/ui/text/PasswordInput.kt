package love.yinlin.compose.ui.text

import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.ui.node.semantics

/**
 * @param state 状态
 * @param hint 提示
 * @param enabled 可写入
 * @param style 文字样式
 * @param mask 密码遮盖符
 * @param onImeClick IME按钮点击事件
 * @param colorProvider 提示灯颜色
 * @param leading 头部装饰
 * @param trailing 尾部装饰
 */
@Composable
fun PasswordInput(
    state: InputState = rememberInputState(),
    modifier: Modifier = Modifier,
    hint: String? = null,
    enabled: Boolean = true,
    style: TextStyle = LocalStyle.current,
    mask: Char = '\u2022',
    onImeClick: (KeyboardActionScope.() -> Unit)? = null,
    colorProvider: InputStatusColorProvider = InputStatusColorProvider.Default,
    leading: InputDecoration? = null,
    trailing: InputDecoration = InputDecoration.PasswordViewer,
) {
    val toolBar = LocalTextToolbar.current
    val disableCopyToolBar = remember(toolBar) {
        object : TextToolbar by toolBar {
            override fun showMenu(
                rect: Rect,
                onCopyRequested: (() -> Unit)?,
                onPasteRequested: (() -> Unit)?,
                onCutRequested: (() -> Unit)?,
                onSelectAllRequested: (() -> Unit)?,
                onAutofillRequested: (() -> Unit)?,
            ) {
                toolBar.showMenu(
                    rect = rect,
                    onPasteRequested = onPasteRequested,
                    onSelectAllRequested = onSelectAllRequested,
                    onCopyRequested = null,
                    onCutRequested = null,
                    onAutofillRequested = onAutofillRequested,
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        state.keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done
        )
    }

    LaunchedEffect(mask) {
        state.visualTransformation = PasswordVisualTransformation(mask)
    }

    CompositionLocalProvider(LocalTextToolbar provides disableCopyToolBar) {
        Input(
            state = state,
            modifier = modifier.semantics(contentType = ContentType.Password),
            hint = hint,
            enabled = enabled,
            style = style,
            maxLines = 1,
            minLines = 1,
            onImeClick = onImeClick,
            colorProvider = colorProvider,
            leading = leading,
            trailing = trailing,
        )
    }
}