package love.yinlin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import androidx.lifecycle.viewmodel.compose.viewModel
import love.yinlin.model.AppModel
import love.yinlin.platform.DesktopContext
import love.yinlin.platform.KV
import love.yinlin.ui.AppTopBar
import org.jetbrains.compose.resources.stringResource
import ylcs_kmp.composeapp.generated.resources.Res
import ylcs_kmp.composeapp.generated.resources.app_name

fun main() {
    val context = DesktopContext()
    val kv = KV()
    application {
        val state = rememberWindowState(
            placement = WindowPlacement.Floating,
            isMinimized = false,
            position = WindowPosition.Aligned(Alignment.Center),
            width = context.windowWidth.dp,
            height = context.windowHeight.dp
        )
        var isOpen by remember { mutableStateOf(true) }
        if (isOpen) {
            Window(
                onCloseRequest = ::exitApplication,
                title = stringResource(Res.string.app_name),
                undecorated = true,
                resizable = false,
                transparent = true,
                state = state,
            ) {
                AppWrapper(context, false) {
                    Column(Modifier.fillMaxSize()) {
                        WindowDraggableArea(Modifier.fillMaxWidth()) {
                            AppTopBar(
                                onMinimized = { state.isMinimized = true },
                                onClosed = { isOpen = false }
                            )
                        }
                        App(viewModel { AppModel(kv) })
                    }
                }
            }
        }
    }
}