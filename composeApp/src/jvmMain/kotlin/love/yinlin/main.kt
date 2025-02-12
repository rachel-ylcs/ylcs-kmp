package love.yinlin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.sun.javafx.application.PlatformImpl
import love.yinlin.ui.component.AppTopBar
import org.jetbrains.compose.resources.stringResource
import ylcs_kmp.composeapp.generated.resources.Res
import ylcs_kmp.composeapp.generated.resources.app_name

fun main() {
    val context = DesktopContext()
    appContext = context.initialize()

    // JavaFx
    PlatformImpl.setImplicitExit(false)

    application {
        val rawDensity = LocalDensity.current
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
                transparent = true,
                state = state,
            ) {
                LaunchedEffect(Unit) {
                    if (context.rawDensity == null) context.rawDensity = rawDensity
                }
                AppWrapper {
                    Column(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(15.dp))) {
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