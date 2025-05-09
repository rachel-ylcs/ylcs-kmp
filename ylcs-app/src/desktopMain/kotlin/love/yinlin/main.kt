package love.yinlin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.sun.jna.Native
import love.yinlin.extension.rememberState
import love.yinlin.platform.ActualAppContext
import love.yinlin.platform.Picker
import love.yinlin.platform.app
import love.yinlin.resources.Res
import love.yinlin.resources.app_name
import love.yinlin.resources.img_logo
import love.yinlin.ui.component.AppTopBar
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.Dimension

fun main() {
    System.setProperty("compose.swing.render.on.graphics", "true")
    System.setProperty("compose.interop.blending", "true")

    ActualAppContext().apply {
        app = this
        initialize()
    }

    application {
        var isOpen by rememberState { true }

        val state = rememberWindowState(
            placement = WindowPlacement.Floating,
            isMinimized = false,
            position = WindowPosition.Aligned(Alignment.Center),
            width = 1200.dp,
            height = 700.dp
        )

        if (isOpen) {
            Window(
                onCloseRequest = ::exitApplication,
                title = stringResource(Res.string.app_name),
                icon = painterResource(Res.drawable.img_logo),
                undecorated = true,
                resizable = true,
                transparent = true,
                state = state,
            ) {
                // MinimumSize
                LaunchedEffect(Unit) {
                    window.minimumSize = Dimension(360, 640)
                    Picker.windowHandle = Native.getWindowID(window)
                }

                // Content
                AppWrapper {
                    Column(modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.extraLarge)) {
                        WindowDraggableArea(modifier = Modifier.fillMaxWidth()) {
                            AppTopBar {
                                Action(
                                    icon = Icons.Outlined.Remove,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    state.isMinimized = true
                                }
                                Action(
                                    icon = Icons.Outlined.Close,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    isOpen = false
                                }
                            }
                        }
                        App(modifier = Modifier.fillMaxWidth().weight(1f))
                    }
                }
            }
        }
    }
}