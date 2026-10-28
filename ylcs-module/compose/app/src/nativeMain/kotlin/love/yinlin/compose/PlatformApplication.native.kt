package love.yinlin.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.registerSkikoComposeImplementation
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.NativeWindow
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.BaseLazyReference
import love.yinlin.extension.catchingDefault
import love.yinlin.foundation.PlatformContext
import love.yinlin.uri.ImplicitUri
import love.yinlin.uri.RegularUri
import love.yinlin.uri.Uri
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.skiko.window.NativeDesktop
import org.jetbrains.skiko.window.NativeWindowOptions
import platform.posix.exit
import kotlin.math.roundToInt

@Stable
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: BaseLazyReference<A>,
    context: PlatformContext,
) : Application<A>(self, context) {
    constructor(self: BaseLazyReference<A>) : this(self, PlatformContext.Instance)

    @Composable
    protected open fun BeginContent() {}

    protected open val title: String = ""

    protected open val icon: DrawableResource? = null

    protected open val initSize: DpSize = DpSize(1200.dp, 700.dp)

    protected open val minSize: DpSize = DpSize(360.dp, 640.dp)

    protected open val transparent: Boolean = true

    protected open val undecorated: Boolean = true

    protected open val roundedCorner: Boolean = true

    @Composable
    protected open fun TopBar(controller: WindowController, onExit: () -> Unit) = DefaultTopBar(controller, onExit)

    val controller by lazy {
        WindowController(initSize = initSize, initTitle = title, initIcon = icon, initRoundedCorner = roundedCorner, initUndecorated = undecorated, minSize = minSize)
    }

    private val windowStarter = LaunchFlag()

    val mainScope = MainScope()

    fun run() {
        @OptIn(InternalComposeUiApi::class)
        registerSkikoComposeImplementation()

        initApplication(scope = mainScope)

        var closeRequested = false
        val onMainWindowClose = {
            if (!closeRequested) {
                closeRequested = true
                destroyPoolBefore()
                controller.close()
            }
        }

        @OptIn(ExperimentalComposeUiApi::class)
        NativeWindow(
            options = NativeWindowOptions(
                title = controller.title,
                width = controller.size.width.value.roundToInt(),
                height = controller.size.height.value.roundToInt(),
                transparent = transparent,
                undecorated = controller.undecorated
            ),
            onCloseRequest = {
                onMainWindowClose()
                true
            },
            onOpen = controller::attach,
            onEvent = controller::onEvent,
            onClose = controller::detach,
        ) {
            LaunchedEffect(Unit) {
                windowStarter { initPoolLater(this) }
            }

            val useRoundedCorner = transparent && controller.undecorated && !controller.maximize && controller.roundedCorner
            val borderShape = if (useRoundedCorner) Theme.shape.v1 else Theme.shape.rectangle
            val windowModifier = if (controller.undecorated) {
                Modifier.fillMaxSize().clip(borderShape).border(Theme.border.v10 / 2, Theme.color.outline.copy(alpha = 0.5f), borderShape)
            }
            else Modifier.fillMaxSize()

            ComposedLayout(modifier = windowModifier) {
                controller.BindIcon()
                controller.BindTray()
                Column(modifier = Modifier.fillMaxSize()) {
                    if (controller.undecorated) {
                        Box(modifier = Modifier.fillMaxWidth().windowDragArea(controller)) {
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

        destroyPool()

        mainScope.cancel()

        exit(0)
    }

    actual fun backHome() { controller.minimize = true }

    actual fun openUri(uri: Uri): Boolean = catchingDefault(false) {
        if (Coroutines.mainImmediateDispatchNeeded) false else NativeDesktop.openUri(uri.toString())
    }

    actual fun copyText(text: String): Boolean = catchingDefault(false) {
        if (Coroutines.mainImmediateDispatchNeeded) false
        else {
            NativeDesktop.setClipboardText(text)
            true
        }
    }

    actual fun implicitFileUri(uri: Uri): ImplicitUri = RegularUri(uri.toString())
}
