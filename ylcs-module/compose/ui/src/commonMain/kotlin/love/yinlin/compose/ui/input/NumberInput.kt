package love.yinlin.compose.ui.input

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.text.Text

/**
 * 固定 2-8 格的数字输入器。格子只负责绘制，隐藏的安全文本输入组件负责 IME 接入，
 * 不提供字符选择、拖拽、上下文菜单或复制粘贴操作。
 *
 * 点击任意格子都聚焦整组输入；仅接受末尾单个数字的追加以及末尾单字符删除。
 * [state] 的初始内容不得超过 [maxNum]，可通过 [NumberInputState.value] 从外部重置。
 *
 * @param padding 数字与方格边框之间的内部留白
 * @param gap 相邻方格之间的距离
 * @param mask 非空时用该字符显示已输入的数字，并切换为数字密码键盘；状态仍保存真实数字
 * @param onImeClick 与普通 Input 相同，返回 true 时执行 IME 默认动作
 */
@Composable
fun NumberInput(
    state: NumberInputState = rememberNumberInputState(),
    modifier: Modifier = Modifier,
    maxNum: Int = 6,
    style: TextStyle = Theme.typography.v5,
    padding: Dp = Theme.padding.e,
    gap: Dp = Theme.padding.e,
    activeColor: Color = Theme.color.primary,
    fullColor: Color = Theme.color.warning,
    mask: Char? = null,
    onImeClick: (() -> Boolean)? = null,
) {
    require(maxNum in 2 .. 8) { "NumberInput maxNum must be between 2 and 8" }

    val value = state.value
    require(value.length <= maxNum) { "NumberInput value must not exceed maxNum" }

    val density = LocalDensity.current
    val lineHeight = if (style.lineHeight.isSpecified) style.lineHeight else {
        if (style.fontSize.isSpecified) style.fontSize else Theme.typography.v5.fontSize
    }
    val cellSize = with(density) { lineHeight.toDp() } + padding * 2

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focused by state.interactionSource.collectIsFocusedAsState()
    val hovered by state.interactionSource.collectIsHoveredAsState()

    val borderColor = when {
        value.length == maxNum -> fullColor
        focused || hovered -> activeColor
        else -> Theme.color.outline
    }

    val transformation = remember(maxNum) {
        InputTransformation {
            val previous = originalText.toString()
            val updated = asCharSequence().toString()
            val appended = updated.length == previous.length + 1 &&
                updated.startsWith(previous) && updated.last() in '0' .. '9' && updated.length <= maxNum
            val deleted = previous.length == updated.length + 1 && previous.startsWith(updated)

            if (updated != previous && !appended && !deleted) revertAllChanges()
            selection = TextRange(length)
        }
    }

    val keyboardOptions = remember(state.imeAction, mask) {
        KeyboardOptions(
            keyboardType = if (mask == null) KeyboardType.Number else KeyboardType.NumberPassword,
            autoCorrectEnabled = false,
            imeAction = state.imeAction
        )
    }
    val keyboardActionHandler = remember(onImeClick) {
        KeyboardActionHandler { perform ->
            if (onImeClick?.invoke() == true) perform()
        }
    }

    Box(
        modifier = modifier
            .hoverable(state.interactionSource)
            .pointerInput(focusRequester, keyboard) {
                detectTapGestures {
                    focusRequester.requestFocus()
                    keyboard?.show()
                }
            }
            .clearAndSetSemantics {
                contentDescription = "数字输入器"
                stateDescription = "${value.length} / $maxNum 位"
                onClick(label = "输入数字") {
                    focusRequester.requestFocus()
                    keyboard?.show()
                    true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (index in 0 ..< maxNum) {
                Box(
                    modifier = Modifier.size(cellSize)
                        .border(Theme.border.v7, borderColor, Theme.shape.v7),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = value.getOrNull(index)?.let { (mask ?: it).toString() } ?: "",
                        style = style,
                        color = if (style.color == Color.Unspecified) Theme.color.onBackground else style.color,
                        maxLines = 1
                    )
                }
            }
        }

        // IME 通道只有一个不可见的触点，既不会绘制文字，也无法被拖拽选中。
        val inputModifier = Modifier.align(Alignment.BottomEnd)
            .size(Theme.border.v7)
            .alpha(0f)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) keyboard?.show() }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                when {
                    event.isCtrlPressed || event.isMetaPressed -> true
                    event.key == Key.Insert || (event.isShiftPressed && event.key == Key.F10) -> true
                    event.isShiftPressed && event.key == Key.Delete -> true
                    event.key == Key.DirectionLeft || event.key == Key.DirectionRight ||
                        event.key == Key.DirectionUp || event.key == Key.DirectionDown ||
                        event.key == Key.MoveHome || event.key == Key.MoveEnd -> true
                    event.key == Key.Delete -> {
                        state.deleteLast()
                        true
                    }
                    else -> false
                }
            }

        val inputTextStyle = style.copy(color = Color.Transparent)
        if (mask == null) {
            BasicTextField(
                state = state.rawState,
                modifier = inputModifier,
                textStyle = inputTextStyle,
                inputTransformation = transformation,
                keyboardOptions = keyboardOptions,
                onKeyboardAction = keyboardActionHandler,
                interactionSource = state.interactionSource,
                cursorBrush = SolidColor(Color.Transparent),
            )
        } else {
            BasicSecureTextField(
                state = state.rawState,
                modifier = inputModifier,
                textStyle = inputTextStyle,
                inputTransformation = transformation,
                keyboardOptions = keyboardOptions,
                onKeyboardAction = keyboardActionHandler,
                interactionSource = state.interactionSource,
                cursorBrush = SolidColor(Color.Transparent),
                textObfuscationMode = TextObfuscationMode.Hidden,
                textObfuscationCharacter = mask,
            )
        }
    }
}
