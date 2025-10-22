package love.yinlin.compose.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.compose.Device
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.ui.floating.FABAction

@Stable
abstract class NavigationScreen<A, E>(manager: ScreenManager) : BasicScreen<A>(manager) {
    abstract val pages: List<E>
    abstract val subs: List<SubScreen>

    var pageIndex by mutableIntStateOf(0)

    @Composable
    protected abstract fun Wrapper(device: Device, index: Int, content: @Composable (Device, Modifier) -> Unit)

    @Composable
    override fun BasicContent() {
        Wrapper(LocalDevice.current, pageIndex) { device, modifier ->
            AnimatedContent(targetState = pageIndex) { index ->
                Box(modifier = modifier) {
                    subs[index].Content(device)
                }
            }

            LaunchedEffect(pageIndex) {
                val subScreen = subs[pageIndex]
                subScreen.firstLoad(
                    update = { launch { subScreen.initialize(true) } },
                    init = { launch { subScreen.initialize(false) } }
                )
            }
        }
    }

    override val fabIcon: ImageVector? get() = subs[pageIndex].fabIcon
    override val fabCanExpand: Boolean get() = subs[pageIndex].fabCanExpand
    override val fabMenus: Array<FABAction> get() = subs[pageIndex].fabMenus
    override suspend fun onFabClick() = subs[pageIndex].onFabClick()

    @Composable
    override fun Floating() {
        subs[pageIndex].Floating()
    }
}

typealias CommonNavigationScreen<E> = NavigationScreen<Unit, E>