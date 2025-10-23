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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import kotlinx.coroutines.launch
import love.yinlin.common.uri.toUri
import love.yinlin.compose.*
import love.yinlin.compose.screen.DeepLink
import love.yinlin.data.MimeType
import love.yinlin.platform.*
import love.yinlin.resources.Res
import love.yinlin.resources.app_name
import love.yinlin.resources.img_logo
import love.yinlin.ui.component.common.AppTopBar
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Rectangle
import kotlin.io.path.Path

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

fun main() {
    LibraryLoader.run { singleInstance() }

    System.setProperty("compose.swing.render.on.graphics", "true")
    System.setProperty("compose.interop.blending", "true")

    appContext = AppContext(PlatformContext)
    val appContext1 = ActualAppContext().apply {
        app = this
        initialize()
    }

    Platform.use(Platform.MacOS) {
        Desktop.getDesktop().setOpenURIHandler { event ->
            DeepLink.openUri(event.uri.toUri())
        }
    }

    application(exitProcessOnExit = true) {
        val scope = rememberCoroutineScope()

        // 主窗口
        val state = rememberWindowState(
            placement = WindowPlacement.Floating,
            isMinimized = false,
            position = WindowPosition.Aligned(Alignment.Center),
            width = 1200.dp,
            height = 700.dp
        )
        var alwaysOnTop by rememberFalse()

        Window(
            onCloseRequest = ::exitApplication,
            title = stringResource(Res.string.app_name),
            icon = painterResource(Res.drawable.img_logo),
            visible = appContext1.windowVisible,
            undecorated = true,
            resizable = true,
            transparent = true,
            alwaysOnTop = alwaysOnTop,
            state = state,
        ) {
            // MinimumSize
            LaunchedEffect(Unit) {
                window.minimumSize = Dimension(360, 640)
                // See https://github.com/JetBrains/compose-multiplatform/issues/1724
                Platform.use(Platform.Windows) {
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
            AppEntry {
                Column(modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.extraLarge)) {
                    WindowDraggableArea(modifier = Modifier.fillMaxWidth()) {
                        AppTopBar(modifier = Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(
                                top = CustomTheme.padding.verticalExtraSpace,
                                bottom = CustomTheme.padding.verticalExtraSpace,
                                start = CustomTheme.padding.horizontalExtraSpace
                            )
                        ) {
                            if (app.config.userProfile?.hasPrivilegeVIPCalendar == true) {
                                Action(
                                    icon = Icons.Outlined.CleaningServices,
                                    tip = "GC",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    System.gc()
                                }
                            }
                            Action(
                                icon = Icons.Outlined.RocketLaunch,
                                tip = "加载更新包",
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
                                icon = if (alwaysOnTop) Icons.Outlined.MobiledataOff else Icons.Outlined.VerticalAlignTop,
                                tip = "窗口置顶",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                alwaysOnTop = !alwaysOnTop
                            }
                            Action(
                                icon = Icons.Outlined.Remove,
                                tip = "最小化到托盘",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                appContext1.windowVisible = false
                            }
                            Action(
                                icon = Icons.Outlined.CropSquare,
                                tip = "最大化",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                state.placement = if (state.placement == WindowPlacement.Floating) WindowPlacement.Maximized else WindowPlacement.Floating
                            }
                            Action(
                                icon = Icons.Outlined.Close,
                                tip = "关闭",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                exitApplication()
                            }
                        }
                    }
                    ScreenEntry(modifier = Modifier.fillMaxWidth().weight(1f))
                }
            }
        }

        // 托盘
        val trayState = rememberTrayState()
        val notification = rememberNotification(
            title = stringResource(Res.string.app_name),
            message = "已隐藏到任务栏托盘中",
            type = Notification.Type.Info
        )
        LaunchedEffect(appContext1.windowVisible) {
            if (!appContext1.windowVisible && appContext1.config.enabledTip) trayState.sendNotification(notification)
        }
        Tray(
            icon = painterResource(Res.drawable.img_logo),
            state = trayState,
            onAction = { appContext1.windowVisible = true }
        )

        // 悬浮歌词
        (appContext1.musicFactory.floatingLyrics as? ActualFloatingLyrics)?.let {
            if (it.isAttached && appContext1.config.enabledFloatingLyrics) it.Content()
        }
    }
}