package love.yinlin.compose

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
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
import love.yinlin.compose.window.DeepLink
import love.yinlin.extension.BaseLazyReference
import love.yinlin.foundation.PlatformContext
import love.yinlin.platform.Platform
import love.yinlin.uri.ImplicitUri
import love.yinlin.uri.RegularUri
import love.yinlin.uri.Uri
import love.yinlin.uri.toJvmUri
import love.yinlin.uri.toUri
import org.jetbrains.compose.resources.DrawableResource
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlin.system.exitProcess

@Stable
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: BaseLazyReference<A>,
    context: PlatformContext,
) : Application<A>(self, context) {
    constructor(self: BaseLazyReference<A>) : this(self, PlatformContext.Instance)

    @Composable
    protected open fun BeginContent() {}

    // compose渲染相关配置

    /**
     * 离屏渲染
     */
    protected open val composeSwingRenderOnGraphics: Boolean = true

    /**
     * 原生View互作层
     */
    protected open val composeInteropBlending: Boolean = true

    // 这些配置都是非状态变量作为初始值表达, 需要监听变化请使用 controller

    /**
     * 标题
     */
    protected open val title: String = ""

    /**
     * 图标
     */
    protected open val icon: DrawableResource? = null

    /**
     * 初始尺寸
     */
    protected open val initSize: DpSize = DpSize(1200.dp, 700.dp)

    /**
     * 最小尺寸
     */
    protected open val minSize: DpSize = DpSize(360.dp, 640.dp)

    /**
     * 圆角窗口
     */
    protected open val roundedCorner: Boolean = true

    /**
     * 标题栏
     */
    @Composable
    protected open fun TopBar(controller: WindowController, onExit: () -> Unit) = DefaultTopBar(controller, onExit)

    /**
     * 多窗口
     */
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
            initRoundedCorner = roundedCorner
        )
    }

    private val windowStarter = LaunchFlag()

    val mainScope = MainScope()

    fun run() {
        // 配置 compose 渲染
        if (composeSwingRenderOnGraphics) System.setProperty("compose.swing.render.on.graphics", "true")
        if (composeInteropBlending) System.setProperty("compose.interop.blending", "true")

        // 配置 macOS 超链接
        Platform.use(Platform.MacOS) {
            Desktop.getDesktop().setOpenURIHandler { event ->
                DeepLink.openUri(event.uri.toUri())
            }
        }

        initApplication(scope = mainScope)

        application(exitProcessOnExit = false) {
            val onMainWindowClose = {
                destroyPoolBefore()
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
                    this@PlatformApplication.windowHandle = window.windowHandle

                    windowStarter { initPoolLater(this) }
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

        destroyPool()

        mainScope.cancel()

        exitProcess(0)
    }

    actual fun backHome() { controller.minimize = true }

    actual fun openUri(uri: Uri): Boolean {
        val desktop = Desktop.getDesktop()
        return if (desktop.isSupported(Desktop.Action.BROWSE)) {
            desktop.browse(uri.toJvmUri())
            true
        } else false
    }

    actual fun copyText(text: String): Boolean {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, null)
        return true
    }

    actual fun implicitFileUri(uri: Uri): ImplicitUri = RegularUri(uri.toString())
}