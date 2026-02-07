package love.yinlin.compose.ui.text

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.zIndex
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.node.shadow

@Composable
internal fun DecorationBox(
    state: InputState,
    hint: String?,
    alignment: Alignment.Vertical,
    enabled: Boolean,
    colorProvider: InputStatusColorProvider,
    leading: InputDecoration?,
    trailing: InputDecoration?,
    innerContent: @Composable () -> Unit
) {
    val minWidth = Theme.size.input3
    val shape = Theme.shape.v8

    val isFocused by state.interactionSource.collectIsFocusedAsState()
    val isHovered by state.interactionSource.collectIsHoveredAsState()
    val isPressed by state.interactionSource.collectIsPressedAsState()
    val isDragged by state.interactionSource.collectIsDraggedAsState()

    val lightColor by animateColorAsState(
        targetValue = if (enabled) colorProvider.color(state, isFocused, isHovered, isPressed, isDragged) else Theme.color.disabledContent,
        animationSpec = tween(Theme.animation.duration.default)
    )
    val backgroundColor = if (enabled) Theme.color.backgroundVariant else Theme.color.disabledContainer

    Row(
        modifier = Modifier
            .defaultMinSize(minWidth = minWidth, minHeight = minWidth * 0.28f)
            .width(IntrinsicSize.Max)
            .shadow(shape, Theme.shadow.v7)
            .clip(shape)
            .background(backgroundColor)
            .border(Theme.border.v6, lightColor, shape)
            .pointerIcon(PointerIcon.Default)
            .hoverable(state.interactionSource)
            .padding(
                horizontal = if (leading != null) Theme.padding.h else Theme.padding.h10,
                vertical = Theme.padding.v10
            ),
        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
    ) {
        if (leading != null) {
            Box(modifier = Modifier.align(leading.alignment)) {
                leading.Content(state)
            }
        }
        Box(modifier = Modifier.weight(1f).align(alignment).pointerIcon(PointerIcon.Text, enabled = enabled)) {
            val alpha by animateFloatAsState(
                targetValue = if (state.isEmpty && hint != null) 0.5f else 0f,
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 500.0f)
            )

            Text(
                text = hint ?: "",
                color = LocalColorVariant.current.copy(alpha = alpha),
                modifier = Modifier.zIndex(1f)
            )

            Box(modifier = Modifier.zIndex(2f)) {
                innerContent()
            }
        }
        if (trailing != null) {
            Box(modifier = Modifier.align(trailing.alignment)) {
                trailing.Content(state)
            }
        }
    }
}

/**
 * @param state 状态
 * @param hint 提示
 * @param enabled 可写入
 * @param style 文字样式
 * @param alignment 文字对齐方式
 * @param maxLines 最大行数
 * @param minLines 最小行数
 * @param imeAction IME图标
 * @param onImeClick IME按钮点击事件
 * @param colorProvider 提示灯颜色
 * @param leading 头部装饰
 * @param trailing 尾部装饰
 */
@Composable
fun Input(
    state: InputState = rememberInputState(),
    modifier: Modifier = Modifier,
    hint: String? = null,
    enabled: Boolean = true,
    style: TextStyle = LocalStyle.current,
    alignment: Alignment.Vertical = Alignment.CenterVertically,
    maxLines: Int = 1,
    minLines: Int = maxLines,
    imeAction: ImeAction = ImeAction.Done,
    onImeClick: (KeyboardActionScope.() -> Unit)? = null,
    colorProvider: InputStatusColorProvider = InputStatusColorProvider.Default,
    leading: InputDecoration? = null,
    trailing: InputDecoration? = null,
) {
    val contentColor = if (enabled) Theme.color.onBackground else Theme.color.disabledContent
    val textStyle = if (style.color == Color.Unspecified) style.copy(color = contentColor) else style

    CompositionLocalProvider(
        LocalColor provides contentColor,
        LocalColorVariant provides Theme.color.onBackgroundVariant,
        LocalStyle provides textStyle
    ) {
        BasicTextField(
            value = state.value,
            onValueChange = { state.update(it) },
            modifier = modifier,
            enabled = enabled,
            readOnly = !enabled,
            textStyle = textStyle,
            singleLine = maxLines.coerceAtLeast(1) == 1,
            minLines = minLines.coerceAtLeast(1),
            maxLines = maxLines.coerceAtLeast(1),
            keyboardOptions = state.keyboardOptions ?: remember(imeAction) {
                KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    autoCorrectEnabled = false,
                    imeAction = imeAction
                )
            },
            keyboardActions = state.keyboardActions ?: remember(onImeClick, maxLines) {
                // 多行输入框回车的功能是换行而不是触发IME
                KeyboardActions(if (onImeClick != null && maxLines == 1) onImeClick else null)
            },
            visualTransformation = state.visualTransformation ?: VisualTransformation.None,
            cursorBrush = remember(contentColor) { SolidColor(contentColor) },
            interactionSource = state.interactionSource,
            decorationBox = { innerContent ->
                DecorationBox(state, hint, alignment, enabled, colorProvider, leading, trailing, innerContent)
            }
        )
    }
}