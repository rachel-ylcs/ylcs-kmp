package love.yinlin.compose.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.*
import love.yinlin.compose.Device
import love.yinlin.compose.LocalDevice

@Stable
abstract class NavigationScreen<A, E>(manager: ScreenManager) : BasicScreen<A>(manager) {
    abstract val pages: List<E>
    abstract val subs: List<SubScreen>

    private var pageIndex by mutableIntStateOf(0)

    @Composable
    protected abstract fun Wrapper(device: Device, page: E, content: @Composable (Device) -> Unit)

    @Composable
    protected abstract fun Content(device: Device, page: E)

    @Composable
    override fun BasicContent() {
        val device = LocalDevice.current
        Wrapper(device, pages[pageIndex]) {
            AnimatedContent(targetState = pageIndex) { index ->
                Content(device, pages[index])
            }
        }
    }

    var current: E get() = pages[pageIndex]
        set(value) { pageIndex = pages.indexOf(value) }
}

typealias CommonNavigationScreen<E> = NavigationScreen<Unit, E>