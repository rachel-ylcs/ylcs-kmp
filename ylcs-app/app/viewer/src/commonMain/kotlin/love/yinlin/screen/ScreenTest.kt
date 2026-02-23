package love.yinlin.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import love.yinlin.compose.screen.Screen

@Stable
class ScreenTest : Screen() {
    override val title: String = "测试页"

    @Composable
    override fun Content() {

    }
}