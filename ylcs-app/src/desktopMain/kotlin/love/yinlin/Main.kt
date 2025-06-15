package love.yinlin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import kotlinx.coroutines.launch
import love.yinlin.common.DeepLinkHandler
import love.yinlin.common.ThemeValue
import love.yinlin.common.toUri
import love.yinlin.data.MimeType
import love.yinlin.platform.*
import love.yinlin.resources.Res
import love.yinlin.resources.app_name
import love.yinlin.resources.img_logo
import love.yinlin.ui.component.AppTopBar
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Rectangle
import kotlin.io.path.Path
import kotlin.system.exitProcess

private object LibraryLoader {
    init {
        // 本机库
        System.loadLibrary("ylcs_native")
        // VLC
        val vlcPath = Path(System.getProperty("compose.application.resources.dir")).parent.parent.let {
            when (platform) {
                Platform.Windows -> it.resolve("vlc")
                Platform.Linux -> it.resolve("bin/vlc")
                Platform.MacOS -> it.resolve("MacOS/vlc")
                else -> it
            }
        }
        System.setProperty("jna.library.path", vlcPath.toString())
    }
}

private external fun requestSingleInstance(): Boolean
private external fun releaseSingleInstance()

fun main() {
    LibraryLoader.run {
        if (!requestSingleInstance()) {
            releaseSingleInstance()
            exitProcess(0)
        }
        Runtime.getRuntime().addShutdownHook(Thread(::releaseSingleInstance))
    }

    System.setProperty("compose.swing.render.on.graphics", "true")
    System.setProperty("compose.interop.blending", "true")

    ActualAppContext().apply {
        app = this
        initialize()
    }

    OS.ifPlatform(Platform.MacOS) {
        Desktop.getDesktop().setOpenURIHandler { event ->
            DeepLinkHandler.onOpenUri(event.uri.toUri())
        }
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
                // See https://github.com/JetBrains/compose-multiplatform/issues/1724
                OS.ifPlatform(Platform.Windows) {
                    val screenBounds = window.graphicsConfiguration.bounds
                    val screenInsets = window.toolkit.getScreenInsets(window.graphicsConfiguration)
                    window.maximizedBounds = Rectangle(
                        screenBounds.x + screenInsets.left,
                        screenBounds.y + screenInsets.top,
                        screenBounds.width - screenInsets.left - screenInsets.right,
                        screenBounds.height - screenInsets.top - screenInsets.bottom
                    )
                }
                Picker.windowHandle = window.windowHandle
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

        (app.musicFactory.floatingLyrics as? ActualFloatingLyrics)?.let {
            if (it.isAttached && app.config.enabledFloatingLyrics) it.Content()
        }
    }
}