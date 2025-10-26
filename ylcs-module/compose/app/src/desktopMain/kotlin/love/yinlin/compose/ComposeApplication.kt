package love.yinlin.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import love.yinlin.compose.ui.floating.localBalloonTipEnabled
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.layout.AppTopBar
import org.jetbrains.compose.resources.*
import java.awt.Dimension

fun composeApplication(
    title: StringResource,
    icon: DrawableResource,
    initSize: DpSize = DpSize(1200.dp, 700.dp),
    minSize: DpSize = DpSize(360.dp, 640.dp),
    onWindowCreate: ComposeWindow.() -> Unit = {},
    actionAlwaysOnTop: Boolean = false,
    actionMinimize: Boolean = true,
    actionMaximize: Boolean = true,
    actionClose: Boolean = true,
    actions: @Composable (ActionScope.() -> Unit) = {},
    tray: Boolean = false,
    trayHideNotification: String? = null,
    trayClick: (show: () -> Unit) -> Unit = { show -> show() },
    multiWindow: @Composable ApplicationScope.() -> Unit = {},
    entry: @Composable (framework: @Composable () -> Unit) -> Unit = { framework -> App { framework() } },
    content: @Composable BoxScope.() -> Unit
) {
    var windowVisible by mutableStateOf(true)
    var alwaysOnTop by mutableStateOf(false)

    val minimumSize = Dimension(minSize.width.value.toInt(), minSize.height.value.toInt())

    application(exitProcessOnExit = true) {
        val state = rememberWindowState(
            placement = WindowPlacement.Floating,
            isMinimized = false,
            position = WindowPosition.Aligned(Alignment.Center),
            size = initSize
        )

        Window(
            onCloseRequest = ::exitApplication,
            title = stringResource(title),
            icon = painterResource(icon),
            visible = windowVisible,
            undecorated = true,
            resizable = true,
            transparent = true,
            alwaysOnTop = alwaysOnTop,
            state = state
        ) {
            LaunchedEffect(minimumSize, onWindowCreate) {
                window.minimumSize = minimumSize
                window.onWindowCreate()
            }

            entry {
                Column(modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.extraLarge)) {
                    WindowDraggableArea(modifier = Modifier.fillMaxWidth()) {
                        AppTopBar(
                            title = stringResource(title),
                            icon = icon,
                            modifier = Modifier.fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(
                                    top = CustomTheme.padding.verticalExtraSpace,
                                    bottom = CustomTheme.padding.verticalExtraSpace,
                                    start = CustomTheme.padding.horizontalExtraSpace
                                )
                        ) {
                            actions()

                            if (actionAlwaysOnTop) {
                                Action(
                                    icon = if (alwaysOnTop) Icons.Outlined.MobiledataOff else Icons.Outlined.VerticalAlignTop,
                                    tip = "窗口置顶",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    alwaysOnTop = !alwaysOnTop
                                }
                            }

                            if (actionMinimize) {
                                Action(
                                    icon = Icons.Outlined.Remove,
                                    tip = "最小化到托盘",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    windowVisible = false
                                }
                            }

                            if (actionMaximize) {
                                Action(
                                    icon = Icons.Outlined.CropSquare,
                                    tip = "最大化",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    state.placement = if (state.placement == WindowPlacement.Floating) WindowPlacement.Maximized else WindowPlacement.Floating
                                }
                            }

                            if (actionClose) {
                                Action(
                                    icon = Icons.Outlined.Close,
                                    tip = "关闭",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    exitApplication()
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        content = content
                    )
                }
            }
        }

        if (tray) {
            val trayState = rememberTrayState()

            Tray(
                icon = painterResource(icon),
                state = trayState,
                onAction = {
                    trayClick { windowVisible = true }
                }
            )

            if (trayHideNotification != null) {
                val notification = rememberNotification(
                    title = stringResource(title),
                    message = trayHideNotification,
                    type = Notification.Type.Info
                )

                val enabledTip = localBalloonTipEnabled.current

                LaunchedEffect(windowVisible, enabledTip) {
                    if (!windowVisible && enabledTip) trayState.sendNotification(notification)
                }
            }
        }

        multiWindow()
    }
}