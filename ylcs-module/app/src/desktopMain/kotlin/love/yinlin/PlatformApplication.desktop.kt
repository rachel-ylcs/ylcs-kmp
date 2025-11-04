package love.yinlin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import love.yinlin.compose.CustomTheme
import love.yinlin.compose.DefaultIcon
import love.yinlin.compose.LaunchFlag
import love.yinlin.compose.ui.floating.localBalloonTipEnabled
import love.yinlin.compose.ui.image.MiniIcon
import love.yinlin.compose.ui.image.MiniImage
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.layout.Space
import love.yinlin.extension.LazyReference
import org.jetbrains.compose.resources.*
import java.awt.Dimension
import kotlin.system.exitProcess

@Stable
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: LazyReference<A>,
    delegate: PlatformContextDelegate,
) : Application<A>(self, delegate) {
    @Composable
    protected open fun BeginContent() {}

    protected open val title: String = ""
    protected open val icon: DrawableResource? = null
    protected open val initSize: DpSize = DpSize(1200.dp, 700.dp)
    protected open val minSize: DpSize = DpSize(360.dp, 640.dp)
    protected open val actionAlwaysOnTop: Boolean = false
    protected open val actionMinimize: Boolean = true
    protected open val actionMaximize: Boolean = true
    protected open val actionClose: Boolean = true

    @Composable
    protected open fun TopBar(actions: @Composable ActionScope.() -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(
                    top = CustomTheme.padding.verticalExtraSpace,
                    bottom = CustomTheme.padding.verticalExtraSpace,
                    start = CustomTheme.padding.horizontalExtraSpace
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val topBarIcon = icon
            if (topBarIcon != null) {
                MiniIcon(topBarIcon)
            }
            else {
                MiniImage(
                    painter = rememberVectorPainter(DefaultIcon),
                    modifier = Modifier.size(CustomTheme.size.icon)
                )
            }
            Space()
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Space()
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End
            ) {
                ActionScope.Right.actions()
            }
        }
    }

    protected open val tray: Boolean = false
    protected open val trayHideNotification: String? = null
    protected open fun onTrayClick(show: () -> Unit) { show() }

    @Composable
    protected open fun ApplicationScope.MultipleWindow() {}

    fun run() {
        initialize(delay = false)

        var windowVisible by mutableStateOf(true)
        var alwaysOnTop by mutableStateOf(false)
        val minimumSize = Dimension(minSize.width.value.toInt(), minSize.height.value.toInt())

        val windowState = WindowState(
            placement = WindowPlacement.Floating,
            isMinimized = false,
            position = WindowPosition.Aligned(Alignment.Center),
            size = initSize
        )

        val windowStarter = LaunchFlag()

        application(exitProcessOnExit = false) {
            Window(
                onCloseRequest = {
                    destroy(delay = true)
                    exitApplication()
                },
                title = title,
                icon = icon?.let { painterResource(it) } ?: rememberVectorPainter(DefaultIcon),
                visible = windowVisible,
                undecorated = true,
                resizable = true,
                transparent = true,
                alwaysOnTop = alwaysOnTop,
                state = windowState
            ) {
                LaunchedEffect(Unit) {
                    window.minimumSize = minimumSize
                    context.bindWindow(window)
                    windowStarter {
                        initialize(delay = true)
                    }
                }

                Layout {
                    Column(modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.extraLarge)) {
                        WindowDraggableArea(modifier = Modifier.fillMaxWidth()) {
                            TopBar {
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
                                        windowState.placement = if (windowState.placement == WindowPlacement.Floating) WindowPlacement.Maximized else WindowPlacement.Floating
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

                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            BeginContent()
                            Content()
                        }
                    }
                }
            }

            if (tray) {
                val trayState = rememberTrayState()

                Tray(
                    icon = icon?.let { painterResource(it) } ?: rememberVectorPainter(DefaultIcon),
                    state = trayState,
                    onAction = {
                        onTrayClick { windowVisible = true }
                    }
                )

                trayHideNotification?.let { notificationText ->
                    val notification = rememberNotification(
                        title = title,
                        message = notificationText,
                        type = Notification.Type.Info
                    )

                    val enabledTip = localBalloonTipEnabled.current

                    LaunchedEffect(windowVisible, enabledTip) {
                        if (!windowVisible && enabledTip) trayState.sendNotification(notification)
                    }
                }
            }

            MultipleWindow()
        }

        destroy(delay = false)

        exitProcess(0)
    }
}