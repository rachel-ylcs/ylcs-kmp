package love.yinlin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import love.yinlin.compose.PlatformApplication
import love.yinlin.compose.ThemeMode
import love.yinlin.compose.ToolingTheme
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.extension.LazyReference
import love.yinlin.foundation.PlatformContextDelegate

class MainApplication : PlatformApplication<MainApplication>(appReference, PlatformContextDelegate()) {
    private var darkMode by mutableStateOf(false)

    override val themeMode: ThemeMode get() = if (darkMode) ThemeMode.DARK else ThemeMode.LIGHT
    override val title: String = "Test"
    override val initSize: DpSize = DpSize(1200.dp, 800.dp)
    override val toolingTheme: ToolingTheme = ToolingTheme(enableBallonTip = true)

    @Composable
    override fun Content() {
        PrimaryButton("测试", onClick = { darkMode = !darkMode })
    }
}

private val appReference = LazyReference<MainApplication>()
val app by appReference

fun main() = MainApplication().run()