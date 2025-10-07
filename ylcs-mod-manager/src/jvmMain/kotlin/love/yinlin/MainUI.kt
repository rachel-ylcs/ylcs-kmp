package love.yinlin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import java.io.File

@Stable
class MainViewModel(
    val rootPath: File
) : ViewModel() {

}

@Composable
fun MainUI(vm: MainViewModel) {

}