@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package love.yinlin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

private val Ink = Color(0xFF182D3B)
private val Teal = Color(0xFF146C69)
private val Background = Color(0xFFF2F5F5)
private val PanelShape = RoundedCornerShape(16.dp)

/** The latest porting stage, using ordinary Foundation and Animation APIs without Material. */
@Composable
internal fun ShowcaseScene() {
    var count by remember { mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf(true) }
    var dialog by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    var dark by remember { mutableStateOf(false) }
    val editor = rememberTextFieldState("你好，Native Foundation 😀")
    val color by animateColorAsState(if (dark) Ink else Background, tween(300))
    val position by animateFloatAsState(if (count % 2 == 0) 0.08f else 0.92f, tween(600))
    val foreground = if (dark) Color.White else Ink

    Column(Modifier.fillMaxSize().background(color).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Label("RACHEL / FOUNDATION + ANIMATION", if (dark) Color(0xFF8ED6CF) else Teal, 14)
        Label("从原生窗口，到交互组件。", foreground, 30)
        Label("布局 · 点击 · 文本编辑 · 选择菜单 · 列表 · 动画", foreground)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Action("组件", tab == 0) { tab = 0 }
            Action("列表", tab == 1) { tab = 1 }
            Action("资源", tab == 2) { tab = 2 }
            Action("导航", tab == 3) { tab = 3 }
            Action(if (dark) "浅色背景" else "深色背景") { dark = !dark }
        }

        if (tab == 0) {
            Panel {
                Label("01 / 状态与动画", Teal)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Action("点击计数 $count") { count++ }
                    Action(if (expanded) "收起说明" else "展开说明") { expanded = !expanded }
                }
                Canvas(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE6F1F0))) {
                    drawCircle(Teal, 12.dp.toPx(), Offset(size.width * position, size.height / 2))
                }
                AnimatedVisibility(expanded) {
                    SelectionContainer {
                        Label("这是实际的 Foundation 布局、clickable、Canvas 与 Animation。点击计数会改变圆点位置；此段文字可选择和复制。")
                    }
                }
            }
            Panel {
                Label("02 / 文本、焦点与菜单", Teal)
                BasicTextField(
                    state = editor,
                    modifier = Modifier.fillMaxWidth().border(1.dp, Teal, RoundedCornerShape(10.dp)).padding(14.dp),
                    textStyle = TextStyle(color = Ink, fontSize = 18.sp),
                    cursorBrush = SolidColor(Teal),
                    lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 2, maxHeightInLines = 4),
                )
                Label("可手动检查中文 IME、AltGr、方向键、撤销、复制粘贴和右键菜单。", size = 13)
                Label("当前字符数：${editor.text.length}", size = 13)
                Action("打开弹层") { dialog = true }
            }
        }
        else if (tab == 1) {
            Panel {
                Label("03 / LazyColumn 与稳定条目", Teal)
                Label("列表在固定高度内独立滚动，外层页面保持自己的滚动状态。", size = 13)
                LazyColumn(Modifier.fillMaxWidth().height(320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(60, key = { it }) { index ->
                        var selected by remember { mutableStateOf(false) }
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Color(0xFFD4EAE7) else Background)
                            .clickable { selected = !selected }.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Label("Native item ${index + 1}")
                            Label(if (selected) "已选择" else "点击选择", Teal)
                        }
                    }
                }
            }
        }
        else if (tab == 2) {
            Panel { ResourcesPreview(count) }
        }
        else NavigationPreview()
    }
    if (dialog) {
        Dialog(onDismissRequest = { dialog = false }) {
            Panel(Modifier.widthIn(max = 480.dp)) {
                Label("Foundation Dialog", Teal, 24)
                Label("继续复用 ComposeScene 弹层、焦点与返回处理。按 Escape 或点击关闭。")
                Action("关闭") { dialog = false }
            }
        }
    }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(PanelShape).background(Color.White).animateContentSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
}

@Composable
private fun Label(text: String, color: Color = Ink, size: Int = 16) {
    BasicText(text, style = TextStyle(color = color, fontSize = size.sp))
}

@Composable
private fun Action(text: String, selected: Boolean = false, onClick: () -> Unit) {
    BasicText(text, modifier = Modifier.clip(RoundedCornerShape(8.dp))
        .background(if (selected) Color(0xFF28578A) else Teal).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        style = TextStyle(color = Color.White, fontSize = 15.sp))
}
