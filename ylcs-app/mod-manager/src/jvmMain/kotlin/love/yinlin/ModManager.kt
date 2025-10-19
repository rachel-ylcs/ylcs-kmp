package love.yinlin

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

fun main(args: Array<String>) {
    val defaultPath = File(System.getProperty("user.dir"))
    val rootPath = args.getOrNull(0)?.let {
        val userPath = File(it)
        if (userPath.isDirectory) userPath
        else defaultPath
    } ?: defaultPath
    singleWindowApplication(
        title = "MOD管理器",
        state = WindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(1200.dp, 800.dp)
        )
    ) {
        val vm = viewModel { MainViewModel(rootPath) }
        MainUI(vm)
    }
}