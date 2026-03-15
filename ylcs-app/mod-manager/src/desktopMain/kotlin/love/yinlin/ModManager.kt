package love.yinlin

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import love.yinlin.compose.PlatformApplication
import love.yinlin.compose.ToolingTheme
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.extension.LazyReference
import love.yinlin.foundation.PlatformContextDelegate
import love.yinlin.fs.File
import love.yinlin.mod.ModFactory
import love.yinlin.screen.ScreenMain
import love.yinlin.screen.ScreenRhyme

class MainApplication : PlatformApplication<MainApplication>(appReference, PlatformContextDelegate()) {
    override val title: String = "MOD管理器 第${ModFactory.VERSION}版"
    override val initSize: DpSize = DpSize(1200.dp, 800.dp)
    override val toolingTheme: ToolingTheme = ToolingTheme(enableBallonTip = true)

    val rootPath = File(System.getProperty("user.dir"))
    val libraryPath = File(rootPath, "library")
    val outputPath = File(rootPath, "output")
    val modPath = File(rootPath, "mod")

    init {
        async(name = "createDirectories") {
            libraryPath.mkdir()
            outputPath.mkdir()
        }
    }

    @Composable
    override fun Content() {
        ScreenManager.Navigation<ScreenMain> {
            screen(::ScreenMain)
            screen(::ScreenRhyme)
        }
    }
}

private val appReference = LazyReference<MainApplication>()
val app by appReference

fun main() = MainApplication().run()