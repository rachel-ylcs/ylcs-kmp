package love.yinlin

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.io.files.Path
import love.yinlin.compose.screen.AppScreen
import love.yinlin.extension.LazyReference
import love.yinlin.extension.mkdir
import love.yinlin.mod.ModFactory
import love.yinlin.screen.ScreenMain
import love.yinlin.screen.ScreenRhyme

class MainApplication : PlatformApplication<MainApplication>(appReference, PlatformContextDelegate) {
    override val title: String = "MOD管理器 第${ModFactory.VERSION}版"
    override val initSize: DpSize = DpSize(1200.dp, 800.dp)
    override val actionMinimize: Boolean = false

    val rootPath = Path(System.getProperty("user.dir"))
    val libraryPath = Path(rootPath, "library")
    val outputPath = Path(rootPath, "output")
    val modPath = Path(rootPath, "mod")

    override fun onCreate() {
        libraryPath.mkdir()
        outputPath.mkdir()
    }

    @Composable
    override fun Content() {
        AppScreen<ScreenMain> {
            screen(::ScreenMain)
            screen(::ScreenRhyme)
        }
    }
}

private val appReference = LazyReference<MainApplication>()
val app by appReference

fun main() = MainApplication().run()