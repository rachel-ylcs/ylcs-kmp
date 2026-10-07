package love.yinlin.compose.screen

import androidx.compose.runtime.Composable
import kotlin.test.assertIs

// 只用于不渲染 UI 的单元测试
internal open class TestScreen : ScreenModel() {
    @Composable
    override fun ModelContent() { }
}

internal inline fun <reified S : ScreenModel> ScreenManager.requireTopScreen(): S = assertIs<S>(store[backStack.last().id])

internal const val TestManagerID = "screen.manager"