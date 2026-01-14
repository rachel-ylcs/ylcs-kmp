package love.yinlin.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.ui.CustomTheme
import love.yinlin.compose.ui.floating.localBalloonTipEnabled
import love.yinlin.compose.ui.icon.M3Icons
import love.yinlin.compose.ui.image.MiniIcon
import love.yinlin.compose.ui.image.MiniImage
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.layout.Space
import love.yinlin.compose.ui.window.DragArea
import love.yinlin.extension.BaseLazyReference
import love.yinlin.foundation.PlatformContextDelegate
import org.jetbrains.compose.resources.*
import kotlin.system.exitProcess

@Stable
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: BaseLazyReference<A>,
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

    @Stable
    private sealed interface MaximizeState {
        val showText: String
        fun toggle(windowState: WindowState): MaximizeState

        @Stable
        data object Normal : MaximizeState {
            override val showText: String = "最大化"

            override fun toggle(windowState: WindowState): MaximizeState {
                val newState = Maximized(windowState.size, windowState.position)
                windowState.placement = WindowPlacement.Maximized
                return newState
            }
        }

        @Stable
        data class Maximized(val lastSize: DpSize, val lastPosition: WindowPosition) : MaximizeState {
            override val showText: String = "还原"

            override fun toggle(windowState: WindowState): MaximizeState {
                windowState.placement = WindowPlacement.Floating
                windowState.size = lastSize
                windowState.position = lastPosition
                return Normal
            }
        }
    }

    private var maximizeState: MaximizeState by mutableRefStateOf(MaximizeState.Normal)

    private var windowVisible by mutableStateOf(true)
    private var alwaysOnTop by mutableStateOf(false)

    private val windowState by derivedStateOf {
        WindowState(
            placement = WindowPlacement.Floating,
            isMinimized = false,
            position = WindowPosition.Aligned(Alignment.Center),
            size = initSize
        )
    }

    private val windowStarter = LaunchFlag()

    fun run() {
        openService(later = false, immediate = false)

        application(exitProcessOnExit = false) {
            Window(
                onCloseRequest = {
                    closeService(before = true, immediate = false)
                    exitApplication()
                },
                title = title,
                icon = icon?.let { painterResource(it) } ?: rememberVectorPainter(DefaultIcon),
                visible = windowVisible,
                undecorated = true,
                resizable = maximizeState is MaximizeState.Normal,
                transparent = true,
                alwaysOnTop = alwaysOnTop,
                state = windowState
            ) {
                LaunchedEffect(Unit) {
                    Fixup.swingWindowMaximizeBounds(window)
                    context.bindWindow(window.windowHandle)

                    windowStarter {
                        openService(later = true, immediate = false)
                    }
                }

                Fixup.swingWindowMinimize(this, minSize)

                Layout {
                    Column(modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.extraLarge)) {
                        DragArea(maximizeState is MaximizeState.Normal) {
                            TopBar {
                                if (actionAlwaysOnTop) {
                                    Action(
                                        icon = if (alwaysOnTop) M3Icons.MobiledataOff else M3Icons.VerticalAlignTop,
                                        tip = "窗口置顶",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) {
                                        alwaysOnTop = !alwaysOnTop
                                    }
                                }

                                if (actionMinimize) {
                                    Action(
                                        icon = M3Icons.Remove,
                                        tip = "最小化到托盘",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) {
                                        windowVisible = false
                                    }
                                }

                                if (actionMaximize) {
                                    Action(
                                        icon = M3Icons.CropSquare,
                                        tip = maximizeState.showText,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) {
                                        maximizeState = maximizeState.toggle(windowState)
                                    }
                                }

                                if (actionClose) {
                                    Action(
                                        icon = M3Icons.Clear,
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

        closeService(before = false, immediate = false)

        exitProcess(0)
    }
}