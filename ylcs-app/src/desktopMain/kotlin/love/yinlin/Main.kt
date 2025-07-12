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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import kotlinx.coroutines.launch
import love.yinlin.common.DeepLinkHandler
import love.yinlin.common.ThemeValue
import love.yinlin.common.toUri
import love.yinlin.data.MimeType
import love.yinlin.extension.rememberFalse
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
                Windows -> it.resolve("vlc")
                Linux -> it.resolve("bin/vlc")
                MacOS -> it.resolve("MacOS/vlc")
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

    val appContext = ActualAppContext().apply {
        app = this
        initialize()
    }

    OS.ifPlatform(MacOS) {
        Desktop.getDesktop().setOpenURIHandler { event ->
            DeepLinkHandler.onOpenUri(event.uri.toUri())
        }
    }

    application(exitProcessOnExit = true) {
        val scope = rememberCoroutineScope()

        // 主窗口
        val state = rememberWindowState(
            placement = WindowPlacement.Floating,
            isMinimized = false,
            position = WindowPosition.Aligned(Center),
            width = 1200.dp,
            height = 700.dp
        )
        var alwaysOnTop by rememberFalse()

        Window(
            onCloseRequest = ::exitApplication,
            title = stringResource(Res.string.app_name),
            icon = painterResource(Res.drawable.img_logo),
            visible = appContext.windowVisible,
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
                OS.ifPlatform(Windows) {
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
                                appContext.windowVisible = false
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
                    App(modifier = Modifier.fillMaxWidth().weight(1f))
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
        LaunchedEffect(appContext.windowVisible) {
            if (!appContext.windowVisible && appContext.config.enabledTip) trayState.sendNotification(notification)
        }
        Tray(
            icon = painterResource(Res.drawable.img_logo),
            state = trayState,
            onAction = { appContext.windowVisible = true }
        )

        // 悬浮歌词
        (appContext.musicFactory.floatingLyrics as? ActualFloatingLyrics)?.let {
            if (it.isAttached && appContext.config.enabledFloatingLyrics) it.Content()
        }
    }
}