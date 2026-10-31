package love.yinlin.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.registerSkikoComposeImplementation
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.NativeWindow
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import love.yinlin.coroutines.Coroutines
import love.yinlin.compose.ui.window.DragArea
import love.yinlin.extension.BaseLazyReference
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.cleaning
import love.yinlin.foundation.PlatformContext
import love.yinlin.uri.ImplicitUri
import love.yinlin.uri.RegularUri
import love.yinlin.uri.Uri
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.skiko.window.NativeDesktop
import org.jetbrains.skiko.window.NativeWindowOptions
import kotlin.math.roundToInt
import kotlin.system.exitProcess

@Stable
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: BaseLazyReference<A>,
    context: PlatformContext,
) : Application<A>(self, context) {
    constructor(self: BaseLazyReference<A>) : this(self, PlatformContext.Instance)

    @Composable
    protected open fun BeginContent() {}

    protected open val commandArgs: Array<String> = []

    protected open val title: String = ""

    protected open val icon: DrawableResource? = null

    protected open val initSize: DpSize = DpSize(1200.dp, 700.dp)

    protected open val minSize: DpSize = DpSize(360.dp, 640.dp)

    protected open val initPosition: WindowPosition = WindowPosition.Aligned(Alignment.Center)

    protected open val initPlacement: WindowPlacement = WindowPlacement.Floating

    protected open val transparent: Boolean = true

    protected open val undecorated: Boolean = true

    protected open val roundedCorner: Boolean = true

    protected open val focusable: Boolean = true

    protected open val resizable: Boolean = true

    protected open val clickThrough: Boolean = false

    @Composable
    protected open fun TopBar(controller: WindowController, onExit: () -> Unit) = DefaultTopBar(controller, onExit)

    val controller by lazy {
        WindowController(initSize = initSize, initTitle = title, initIcon = icon, initRoundedCorner = roundedCorner, initUndecorated = undecorated, minSize = minSize, placement = initPlacement, position = initPosition, initFocusable = focusable, initResizable = resizable, initClickThrough = clickThrough)
    }

    private val windowStarter = LaunchFlag()

    val mainScope = MainScope()

    private val windows = ApplicationWindowManager(this)

    fun openWindow(window: ApplicationWindow) = windows.open(window)

    fun run() {
        @OptIn(InternalComposeUiApi::class)
        registerSkikoComposeImplementation()

        var closeRequested = false
        val onMainWindowClose = {
            if (!closeRequested) {
                closeRequested = true

                cleaning(::destroyPoolBefore) {
                    cleaning(windows::close, controller::close)
                }
            }
        }

        cleaning({
            initApplication(scope = mainScope)

            @OptIn(ExperimentalComposeUiApi::class)
            NativeWindow(
                options = NativeWindowOptions(
                    title = controller.title,
                    width = controller.size.width.value.roundToInt(),
                    height = controller.size.height.value.roundToInt(),
                    transparent = transparent,
                    undecorated = controller.undecorated,
                    focusable = controller.focusable,
                    resizable = controller.resizable,
                    clickThrough = controller.clickThrough
                ),
                onCloseRequest = {
                    onMainWindowClose()
                    true
                },
                onOpen = { window ->
                    controller.attach(window)
                    openWindowContext(window)
                },
                onEvent = controller::onEvent,
                onClose = {
                    closeWindowContext()
                    controller.detach()
                },
                applicationContent = { windows.Content() },
            ) {
                LaunchedEffect(Unit) {
                    windowStarter { initPoolLater(this) }
                }

                val useRoundedCorner = transparent && controller.undecorated && !controller.maximize && controller.roundedCorner
                val borderShape = if (useRoundedCorner) Theme.shape.v1 else Theme.shape.rectangle
                val windowModifier = if (controller.undecorated && controller.placement != WindowPlacement.Fullscreen) {
                    Modifier.fillMaxSize().clip(borderShape).border(Theme.border.v10 / 2, Theme.color.outline.copy(alpha = 0.5f), borderShape)
                }
                else Modifier.fillMaxSize()

                ComposedLayout(modifier = windowModifier) {
                    controller.BindIcon()
                    controller.BindTray()
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (controller.undecorated && controller.placement != WindowPlacement.Fullscreen) {
                            DragArea(enabled = !controller.maximize, onDoubleClick = { controller.toggleMaximize() }, modifier = Modifier.fillMaxWidth()) {
                                TopBar(controller, onMainWindowClose)
                            }
                        }
                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            BeginContent()
                            Content()
                        }
                    }
                }
            }
        }) {
            cleaning({
                windows.close()
                if (!closeRequested) destroyPoolBefore()
            }) {
                cleaning(::destroyPool) {
                    cleaning(mainScope::cancel) {
                        exitProcess(0)
                    }
                }
            }
        }
    }

    actual fun backHome() { controller.minimize = true }

    actual suspend fun openUri(uri: Uri): Boolean = Coroutines.mainImmediate {
        catchingDefault(false) { NativeDesktop.openUri(uri.toString()) }
    }

    actual suspend fun copyText(text: String): Boolean = Coroutines.mainImmediate {
        catchingDefault(false) {
            NativeDesktop.setClipboardText(text)
            true
        }
    }

    actual fun implicitFileUri(uri: Uri): ImplicitUri = RegularUri(uri.toString())
}
