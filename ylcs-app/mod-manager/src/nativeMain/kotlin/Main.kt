import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.compose.PlatformApplication
import love.yinlin.compose.ToolingTheme
import love.yinlin.compose.image.ImageManager
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.extension.LazyReference
import love.yinlin.fs.File
import love.yinlin.mod.ModFactory
import love.yinlin.screen.ScreenMain
import love.yinlin.screen.ScreenRhyme

class MainApplication(override val commandArgs: Array<String>) : PlatformApplication<MainApplication>(appReference) {
    override val title: String = "MOD管理器 第${ModFactory.VERSION}版"
    override val initSize: DpSize = DpSize(1200.dp, 800.dp)
    override val toolingTheme: ToolingTheme = ToolingTheme(enableBallonTip = true)

    val rootPath = File(SystemFileSystem.resolve(Path(".")).toString())
    val libraryPath = File(rootPath, "library")
    val outputPath = File(rootPath, "output")
    val modPath = File(rootPath, "mod")

    init {
        libraryPath.mkdirSync()
        outputPath.mkdirSync()

        sync {
            ImageManager.initialize(rawContext, "ModManager")
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

fun main(args: Array<String>) = MainApplication(args).run()
