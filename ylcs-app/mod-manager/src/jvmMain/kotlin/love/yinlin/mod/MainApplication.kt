package love.yinlin.mod

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.io.files.Path
import love.yinlin.PlatformApplication
import love.yinlin.PlatformContextDelegate
import love.yinlin.compose.screen.AppScreen
import love.yinlin.extension.Reference
import love.yinlin.extension.mkdir

class MainApplication : PlatformApplication<MainApplication>(appReference, PlatformContextDelegate) {
    override val title: String = "MOD管理器 第${ModFactory.VERSION}版"
    override val initSize: DpSize = DpSize(1200.dp, 800.dp)
    override val actionMaximize: Boolean = false

    val rootPath = Path(System.getProperty("user.dir"))
    val modPath = Path(rootPath, "mod")
    val outputPath = Path(rootPath, "output")

    override fun onCreate() {
        modPath.mkdir()
        outputPath.mkdir()
    }

    @Composable
    override fun Content() {
        AppScreen<MainUI> { screen(::MainUI) }
    }
}

private val appReference = Reference<MainApplication?>(null)
val app: MainApplication get() = appReference.value!!