package love.yinlin.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.window.DragArea
import love.yinlin.extension.BaseLazyReference
import love.yinlin.foundation.PlatformContextDelegate
import org.jetbrains.compose.resources.DrawableResource
import kotlin.system.exitProcess

@Stable
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: BaseLazyReference<A>,
    delegate: PlatformContextDelegate,
) : Application<A>(self, delegate) {
    @Composable
    protected open fun BeginContent() {}

    // 这些配置都是非状态变量作为初始值表达, 需要监听变化请使用 controller

    protected open val title: String = ""
    protected open val icon: DrawableResource? = null
    protected open val initSize: DpSize = DpSize(1200.dp, 700.dp)
    protected open val minSize: DpSize = DpSize(360.dp, 640.dp)
    protected open val roundedCorner: Boolean = true
    protected open val actionAlwaysOnTop: Boolean = false
    protected open val actionMinimize: Boolean = true
    protected open val actionMaximize: Boolean = true
    protected open val actionClose: Boolean = true

    @Composable
    protected open fun TopBar(controller: WindowController, onExit: () -> Unit) = DefaultTopBar(controller, onExit)

    @Composable
    protected open fun ApplicationScope.MultipleWindow() {}

    val controller by lazy {
        WindowController(
            placement = WindowPlacement.Floating,
            isMinimized = false,
            position = WindowPosition.Aligned(Alignment.Center),
            initSize = initSize,
            initTitle = title,
            initIcon = icon,
            initRoundedCorner = roundedCorner,
            initActionAlwaysOnTop = actionAlwaysOnTop,
            initActionMinimize = actionMinimize,
            initActionMaximize = actionMaximize,
            initActionClose = actionClose,
        )
    }

    private val windowStarter = LaunchFlag()

    private val mainScope = MainScope()

    fun run() {
        openService(scope = mainScope, later = false, immediate = false)

        application(exitProcessOnExit = false) {
            val onMainWindowClose = {
                closeService(before = true, immediate = false)
                exitApplication()
            }

            // 主窗口
            Window(
                onCloseRequest = onMainWindowClose,
                title = controller.title,
                icon = controller.iconPainter,
                visible = controller.visible,
                undecorated = true,
                resizable = !controller.maximize,
                transparent = true,
                alwaysOnTop = controller.alwaysOnTop,
                state = controller.rawState,
            ) {
                LaunchedEffect(Unit) {
                    Fixup.swingWindowMaximizeBounds(window)
                    context.bindWindow(window.windowHandle)

                    windowStarter {
                        openService(scope = this, later = true, immediate = false)
                    }
                }

                Fixup.swingWindowMinimize(this, minSize)

                val useRoundedCorner by rememberDerivedState {
                    if (controller.maximize) false else controller.roundedCorner
                }

                ComposedLayout(modifier = Modifier.fillMaxSize().condition(useRoundedCorner) { clip(Theme.shape.v1) }) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        DragArea(
                            enabled = !controller.maximize,
                            onDoubleClick = { controller.toggleMaximize() },
                        ) {
                            TopBar(controller, onMainWindowClose)
                        }

                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            BeginContent()
                            Content()
                        }
                    }
                }
            }

            // 托盘
            val tray = controller.tray
            if (tray.visible) {
                Tray(
                    icon = tray.painter ?: controller.iconPainter,
                    state = tray.state,
                    tooltip = controller.title,
                    onAction = { tray.onDoubleClick?.invoke() }
                )
            }

            // 多窗口
            MultipleWindow()
        }

        closeService(before = false, immediate = false)

        mainScope.cancel()

        exitProcess(0)
    }
}