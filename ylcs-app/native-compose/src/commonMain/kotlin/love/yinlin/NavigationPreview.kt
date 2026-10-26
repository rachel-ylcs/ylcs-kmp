package love.yinlin

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay

/** Exercises the same entry decorators used by Rachel without migrating its screen module. */
@Composable
internal fun NavigationPreview() {
    val backStack = remember { mutableStateListOf(0) }
    var nextId by remember { mutableIntStateOf(1) }
    Column(Modifier.fillMaxWidth().background(Color.White).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicText("Navigation3 · 独立页面状态与 ViewModel")
        BasicText("栈：${backStack.joinToString(" → ")}。进入下一页再返回，计数与输入应保留。")
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxWidth().height(280.dp),
            onBack = { if (backStack.size > 1) backStack.removeLast() },
            entryDecorators = [rememberSaveableStateHolderNavEntryDecorator<Int>(), rememberViewModelStoreNavEntryDecorator<Int>()],
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            popTransitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            entryProvider = { id ->
                NavEntry(id) {
                    val model = viewModel { NavigationPreviewViewModel() }
                    val text = rememberTextFieldState()
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        BasicText("页面 $id · ViewModel 计数 ${model.count}")
                        BasicTextField(state = text, modifier = Modifier.fillMaxWidth().background(Color(0xFFF2F5F5)).padding(12.dp))
                        BasicText("增加计数", Modifier.clickable { model.count++ }.padding(8.dp))
                        BasicText("进入下一页", Modifier.clickable { backStack.add(nextId++) }.padding(8.dp))
                        if (backStack.size > 1) BasicText("返回（也可按 Escape）", Modifier.clickable { backStack.removeLast() }.padding(8.dp))
                    }
                }
            },
        )
    }
}

@Stable
private class NavigationPreviewViewModel : ViewModel() {
    var count by mutableIntStateOf(0)
}
