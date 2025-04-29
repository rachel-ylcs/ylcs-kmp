package love.yinlin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import love.yinlin.extension.rememberState
import love.yinlin.platform.ActualAppContext
import love.yinlin.platform.app
import love.yinlin.resources.Res
import love.yinlin.resources.app_name
import love.yinlin.resources.img_logo
import love.yinlin.ui.component.AppTopBar
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

fun main() {
    System.setProperty("compose.swing.render.on.graphics", "true")
    System.setProperty("compose.interop.blending", "true")

    val context = ActualAppContext()
    app = context
    context.initialize()

    application {
        var isOpen by rememberState { true }

        val state = rememberWindowState(
            placement = WindowPlacement.Floating,
            isMinimized = false,
            position = WindowPosition.Aligned(Alignment.Center),
            width = 1200.dp,
            height = 600.dp
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
                AppWrapper {
                    Column(modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.extraLarge)) {
                        WindowDraggableArea(modifier = Modifier.fillMaxWidth()) {
                            AppTopBar(
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                actions = {
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
                            )
                        }
                        App(modifier = Modifier.fillMaxWidth().weight(1f))
                    }
                }
            }
        }
    }
}