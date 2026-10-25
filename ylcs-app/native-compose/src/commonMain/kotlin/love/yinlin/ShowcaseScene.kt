@file:Suppress("DEPRECATION")
@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package love.yinlin

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt
import kotlin.math.sin

private val Ink = Color(0xFF182D3B)
private val Teal = Color(0xFF146C69)

/** UI primitives only: Foundation and Animation belong to the next porting stage. */
@Composable
internal fun ShowcaseScene() {
    var count by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf(false) }
    var dark by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf(TextFieldValue("你好，Native Compose")) }
    var status by remember { mutableStateOf("点击输入区，验证中文组合输入、方向键、删除和 Ctrl / ⌘ + A") }
    val clipboard = LocalClipboardManager.current
    val background = if (dark) Color(0xFF182D3B) else Color(0xFFF2F5F5)
    val foreground = if (dark) Color(0xFFF1F6F6) else Ink

    LabViewport(background) {
        LabColumn {
            LabText("RACHEL  /  COMPOSE NATIVE", color = if (dark) Color(0xFF8ED6CF) else Teal, fontSize = 14)
            LabText("原生窗口，真实 Compose。", color = foreground, fontSize = 30)
            LabText("重组 · 帧时钟 · 焦点 · 中文输入 · 剪贴板 · Dialog", color = foreground)
            LabColumn(Modifier.drawBehind { drawRoundRect(Color.White, cornerRadius = CornerRadius(18.dp.toPx())) }) {
                LabText("01  状态与键盘交互", color = Teal)
                LabButton("点击计数  $count") { count++ }
                LabButton(if (dark) "切换浅色背景" else "切换深色背景") { dark = !dark }
                LabText("02  文本输入与系统剪贴板", color = Teal)
                LabEditor(value) { value = it }
                LabButton("复制输入内容") {
                    clipboard.setText(AnnotatedString(value.text))
                    status = "已复制；切换到其他程序也可粘贴"
                }
                LabButton("粘贴剪贴板文本") {
                    val text = clipboard.getText()?.text
                    if (text != null) value = TextFieldValue(text, TextRange(text.length))
                    status = if (text == null) "剪贴板没有文本" else "已粘贴"
                }
                LabText(status, fontSize = 12)
            }
            LabColumn(Modifier.drawBehind { drawRoundRect(Color.White, cornerRadius = CornerRadius(18.dp.toPx())) }) {
                LabText("03  Compose 帧时钟与弹层", color = Teal)
                LabPulse()
                LabButton("打开 Dialog · Escape 关闭") { dialog = true }
            }
        }
    }
    if (dialog) {
        Dialog(onDismissRequest = { dialog = false }) {
            LabColumn(Modifier.drawBehind { drawRoundRect(Color.White, cornerRadius = CornerRadius(18.dp.toPx())) }) {
                LabText("Compose Dialog", fontSize = 24)
                LabText("弹层与主页面共用原生窗口，独立处理焦点和返回。")
                LabButton("关闭") { dialog = false }
            }
        }
    }
}

@Composable
private fun LabViewport(background: Color, content: @Composable () -> Unit) {
    var scroll by remember { mutableFloatStateOf(0f) }
    var maximum by remember { mutableFloatStateOf(0f) }
    Layout(content, Modifier.clipToBounds().drawBehind { drawRect(background) }.onPointerEvent(PointerEventType.Scroll) {
        scroll = (scroll + it.changes.sumOf { change -> change.scrollDelta.y.toDouble() }.toFloat() * 40f).coerceIn(0f, maximum)
    }) { measurables, constraints ->
        val child = measurables.single().measure(Constraints(maxWidth = constraints.maxWidth))
        maximum = (child.height - constraints.maxHeight).coerceAtLeast(0).toFloat()
        layout(constraints.maxWidth, constraints.maxHeight) { child.placeRelative(0, -scroll.coerceAtMost(maximum).roundToInt()) }
    }
}

@Composable
private fun LabColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val padding = 20.dp.roundToPx()
        val gap = 12.dp.roundToPx()
        val width = constraints.maxWidth.coerceAtMost(1000.dp.roundToPx())
        val children = measurables.map { it.measure(Constraints(maxWidth = (width - padding * 2).coerceAtLeast(0))) }
        val height = constraints.constrainHeight(padding * 2 + children.sumOf { it.height } + gap * (children.size - 1).coerceAtLeast(0))
        layout(constraints.constrainWidth(width), height) {
            var y = padding
            for (child in children) {
                child.placeRelative(padding, y)
                y += child.height + gap
            }
        }
    }
}

@Composable
private fun LabText(text: String, color: Color = Ink, fontSize: Int = 15, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val style = TextStyle(color = color, fontSize = fontSize.sp)
    Layout(content = {}, modifier = modifier.semantics { this.text = AnnotatedString(text) }.drawWithCache {
        val result = measurer.measure(text, style, constraints = Constraints(maxWidth = size.width.toInt().coerceAtLeast(1)))
        onDrawBehind { drawText(result) }
    }) { _, constraints ->
        val result = measurer.measure(text, style, constraints = constraints)
        layout(result.size.width, result.size.height) {}
    }
}

@Composable
private fun LabButton(label: String, onClick: () -> Unit) {
    val requester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    LabText(label, Color.White, modifier = Modifier.focusRequester(requester).onFocusChanged { focused = it.isFocused }
        .onKeyEvent {
            if (it.type == KeyEventType.KeyUp && (it.key == Key.Enter || it.key == Key.Spacebar)) {
                onClick()
                true
            }
            else false
        }.onPointerEvent(PointerEventType.Press) {
            if (it.buttons.isPrimaryPressed) {
                requester.requestFocus()
                onClick()
            }
        }.focusTarget().semantics {
            role = Role.Button
            onClick {
                onClick()
                true
            }
        }.drawBehind { drawRoundRect(if (focused) Color(0xFF28578A) else Teal, cornerRadius = CornerRadius(8.dp.toPx())) }.labInset())
}

private fun Modifier.labInset(): Modifier = layout { measurable, constraints ->
    val padding = 12.dp.roundToPx()
    val child = measurable.measure(constraints.offset(-padding * 2, -padding * 2))
    layout(child.width + padding * 2, child.height + padding * 2) { child.placeRelative(padding, padding) }
}

@Composable
private fun LabPulse() {
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { phase = (it - start) / 1_000_000_000f }
    }
    Layout(content = {}, modifier = Modifier.drawBehind {
        drawRoundRect(Color(0xFFE6F1F0), cornerRadius = CornerRadius(12.dp.toPx()))
        drawCircle(Teal, 12.dp.toPx(), Offset(size.width * (0.5f + 0.4f * sin(phase)), size.height / 2))
    }) { _, constraints -> layout(constraints.maxWidth, 44.dp.roundToPx()) {} }
}

@Composable
private fun LabEditor(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit) {
    val requester = remember { FocusRequester() }
    val service = LocalTextInputService.current
    val processor = remember { EditProcessor() }
    var focused by remember { mutableStateOf(false) }
    var session by remember { mutableStateOf<TextInputSession?>(null) }
    val currentValue by rememberUpdatedState(value)
    val currentChange by rememberUpdatedState(onValueChange)
    val measurer = rememberTextMeasurer()
    SideEffect { processor.reset(value, session) }
    DisposableEffect(focused, service) {
        val active = if (focused) service?.startInput(currentValue, ImeOptions(singleLine = true),
            onEditCommand = { currentChange(processor.apply(it)) }, onImeActionPerformed = {}) else null
        session = active
        onDispose {
            active?.dispose()
            if (session === active) session = null
        }
    }
    Layout(content = {}, modifier = Modifier.focusRequester(requester).onFocusChanged { focused = it.isFocused }
        .onPointerEvent(PointerEventType.Press) { if (it.buttons.isPrimaryPressed) requester.requestFocus() }
        .onKeyEvent {
            if (it.type != KeyEventType.KeyDown || value.composition != null) false
            else {
                val commands: List<EditCommand> = when {
                    (it.isCtrlPressed || it.isMetaPressed) && it.key == Key.A -> [SetSelectionCommand(0, value.text.length)]
                    it.key == Key.Backspace -> if (!value.selection.collapsed) [CommitTextCommand("", 1)] else [DeleteSurroundingTextInCodePointsCommand(1, 0)]
                    it.key == Key.Delete -> if (!value.selection.collapsed) [CommitTextCommand("", 1)] else [DeleteSurroundingTextInCodePointsCommand(0, 1)]
                    it.key == Key.DirectionLeft -> [MoveCursorCommand(-1)]
                    it.key == Key.DirectionRight -> [MoveCursorCommand(1)]
                    else -> []
                }
                if (commands.isEmpty()) false
                else {
                    onValueChange(processor.apply(commands))
                    true
                }
            }
        }.focusTarget().semantics { editableText = AnnotatedString(value.text) }
        .onGloballyPositioned { session?.notifyFocusedRect(it.boundsInRoot()) }
        .clipToBounds().drawWithCache {
            val result = measurer.measure(value.text, TextStyle(color = Ink, fontSize = 17.sp), softWrap = false)
            val inset = 12.dp.toPx()
            onDrawBehind {
                drawRoundRect(if (focused) Color(0xFFDDEEF7) else Color(0xFFEAF1F1), cornerRadius = CornerRadius(8.dp.toPx()))
                if (!value.selection.collapsed) {
                    val path = result.getPathForRange(value.selection.min, value.selection.max)
                    path.translate(Offset(inset, inset))
                    drawPath(path, Color(0xFFB4D6ED))
                }
                drawText(result, topLeft = Offset(inset, inset))
                if (focused) {
                    val caret = result.getCursorRect(value.selection.end)
                    drawLine(Teal, caret.topLeft + Offset(inset, inset), caret.bottomLeft + Offset(inset, inset), 2.dp.toPx())
                }
            }
        }) { _, constraints -> layout(constraints.maxWidth, 50.dp.roundToPx()) {} }
}
