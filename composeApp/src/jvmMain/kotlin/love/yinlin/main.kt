package love.yinlin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import love.yinlin.component.AppTopBar
import org.jetbrains.compose.resources.stringResource
import ylcs_kmp.composeapp.generated.resources.Res
import ylcs_kmp.composeapp.generated.resources.app_name

fun main() {
    val context = DesktopContext()
    appContext = context.initialize()
    application {
        val state = rememberWindowState(
            placement = WindowPlacement.Floating,
            isMinimized = false,
            position = WindowPosition.Aligned(Alignment.Center),
            width = context.windowWidth.dp,
            height = context.windowHeight.dp
        )
        var isOpen by rememberSaveable { mutableStateOf(true) }
        if (isOpen) {
            Window(
                onCloseRequest = ::exitApplication,
                title = stringResource(Res.string.app_name),
                undecorated = true,
                resizable = false,
                transparent = false,
                state = state,
            ) {
                AppWrapper {
                    Column(modifier = Modifier.fillMaxSize()) {
                        WindowDraggableArea(modifier = Modifier.fillMaxWidth()) {
                            AppTopBar(
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                onMinimized = { state.isMinimized = true },
                                onClosed = { isOpen = false }
                            )
                        }
                        App(modifier = Modifier.fillMaxWidth().weight(1f))
                    }
                }
            }
        }
    }
}