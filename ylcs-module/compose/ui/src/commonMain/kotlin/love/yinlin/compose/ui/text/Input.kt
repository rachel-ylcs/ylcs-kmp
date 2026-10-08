package love.yinlin.compose.ui.text

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.zIndex
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.interaction.collectState
import love.yinlin.compose.ui.node.fastAnimateAlpha
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

    val interactionState by state.interactionSource.collectState(
        colorProvider.useFocused, colorProvider.useHovered, colorProvider.usePressed, colorProvider.useDragged
    )

    val lightColor by animateColorAsState(
        targetValue = if (enabled) colorProvider.color(state, interactionState) else Theme.color.disabledContent,
        animationSpec = tween(Theme.animation.duration.default)
    )
    val backgroundColor = if (enabled) Theme.color.backgroundVariant else Theme.color.disabledContainer

    Row(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .defaultMinSize(minWidth = minWidth, minHeight = minWidth * 0.28f)
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
            Text(
                text = hint ?: "",
                color = LocalColorVariant.current,
                modifier = Modifier.fastAnimateAlpha(if (state.isEmpty && hint != null) 0.5f else 0f).zIndex(1f)
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
    val textStyle = if (style.color == Color.Unspecified) style.copy(color = contentColor) else style

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
                    if (onImeClick?.invoke() == true) {
                        perform()
                        println("fuck")
                    }
                }
            },
            lineLimits = state.lineLimits,
            interactionSource = state.interactionSource,
            cursorBrush = remember(contentColor) { SolidColor(contentColor) },
            outputTransformation = null,
            decorator = { innerContent ->
                DecorationBox(state, hint, alignment, enabled, colorProvider, leading, trailing, innerContent)
            },
            scrollState = state.scrollState
        )
    }
}