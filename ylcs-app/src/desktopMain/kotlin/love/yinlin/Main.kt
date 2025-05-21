package love.yinlin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.sun.jna.Native
import kotlinx.coroutines.launch
import love.yinlin.common.ThemeValue
import love.yinlin.data.MimeType
import love.yinlin.platform.ActualAppContext
import love.yinlin.platform.Coroutines
import love.yinlin.platform.OS
import love.yinlin.platform.Picker
import love.yinlin.platform.Platform
import love.yinlin.platform.app
import love.yinlin.resources.Res
import love.yinlin.resources.app_name
import love.yinlin.resources.img_logo
import love.yinlin.ui.component.AppTopBar
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.Dimension
import java.awt.Rectangle

fun main() {
    System.setProperty("compose.swing.render.on.graphics", "true")
    System.setProperty("compose.interop.blending", "true")

    ActualAppContext().apply {
        app = this
        initialize()
    }

    application(exitProcessOnExit = true) {
        val scope = rememberCoroutineScope()

        val state = rememberWindowState(
            placement = WindowPlacement.Floating,
            isMinimized = false,
            position = WindowPosition.Aligned(Alignment.Center),
            width = 1200.dp,
            height = 700.dp
        )

        Window(
            onCloseRequest = {},
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
                        AppTopBar(modifier = Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(
                                top = ThemeValue.Padding.VerticalExtraSpace,
                                bottom = ThemeValue.Padding.VerticalExtraSpace,
                                start = ThemeValue.Padding.HorizontalExtraSpace
                            )
                        ) {
                            Action(
                                icon = Icons.Outlined.RocketLaunch,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                scope.launch {
                                    Picker.pickPath(mimeType = listOf(MimeType.ZIP), filter = listOf("*.zip"))?.let { path ->
                                        Coroutines.io {
                                            AutoUpdate.start(path.path)
                                        }
                                    }
                                }
                            }
                            Action(
                                icon = Icons.Outlined.Remove,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                state.isMinimized = true
                            }
                            Action(
                                icon = Icons.Outlined.CropSquare,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                state.placement = if (state.placement == WindowPlacement.Floating) WindowPlacement.Maximized else WindowPlacement.Floating
                                // See https://github.com/JetBrains/compose-multiplatform/issues/1724
                                OS.ifPlatform(Platform.Windows) {
                                    if (state.placement == WindowPlacement.Maximized) {
                                        val screenBounds = window.graphicsConfiguration.bounds
                                        val screenInsets = window.toolkit.getScreenInsets(window.graphicsConfiguration)
                                        window.maximizedBounds = Rectangle(
                                            screenBounds.x + screenInsets.left,
                                            screenBounds.y + screenInsets.top,
                                            screenBounds.width - screenInsets.left - screenInsets.right,
                                            screenBounds.height - screenInsets.top - screenInsets.bottom
                                        )
                                    }
                                }
                            }
                            Action(
                                icon = Icons.Outlined.Close,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                exitApplication()
                            }
                        }
                    }
                    App(modifier = Modifier.fillMaxWidth().weight(1f))
                }
            }
        }

        app.musicFactory.floatingLyrics?.let {
            if (it.isAttached) it.Content()
        }
    }
}