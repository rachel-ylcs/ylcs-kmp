package love.yinlin.compose.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import love.yinlin.compose.Device
import love.yinlin.compose.LaunchFlag
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.ui.floating.FABLayout
import kotlin.reflect.KClass

@Stable
abstract class NavigationScreen(manager: ScreenManager) : BasicScreen(manager) {
    @Stable
    data class SubScreenInfo(
        val screen: SubScreen,
        val clz: KClass<out SubScreen>,
        val flag: LaunchFlag = LaunchFlag()
    )

    abstract val subs: List<SubScreenInfo>

    inline fun <reified S : SubScreen> sub(factory: (BasicScreen) -> S): SubScreenInfo = SubScreenInfo(factory(this), S::class)

    inline fun <reified S : SubScreen> get(): S = subs.first { it.clz == S::class }.screen as S

    private val pagerState = PagerState { subs.size }

    var pageIndex: Int get() = pagerState.settledPage
        set(value) {
            launch { pagerState.scrollToPage(value) }
        }

    private val currentScreen: SubScreen get() = subs[pageIndex].screen

    @Composable
    protected abstract fun Wrapper(device: Device, index: Int, content: @Composable (Device) -> Unit)

    @Composable
    override fun BasicContent() {
        Wrapper(LocalDevice.current, pagerState.currentPage) { device ->
            HorizontalPager(
                state = pagerState,
                key = { it },
                beyondViewportPageCount = 0,
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize()
            ) { index ->
                Box(modifier = Modifier.fillMaxSize()) {
                    val screen = subs[index].screen
                    screen.Content(device)

                    screen.fabIcon?.let { icon ->
                        FABLayout(
                            icon = icon,
                            canExpand = screen.fabCanExpand,
                            onClick = screen::onFabClick,
                            menus = screen.fabMenus
                        )
                    }
                }
            }

            LaunchedEffect(pageIndex) {
                val info = subs[pageIndex]
                info.flag({
                    launch { info.screen.update() }
                }) {
                    launch { info.screen.initialize() }
                }
            }
        }
    }

    @Composable
    override fun Floating() = currentScreen.ComposedFloating()
}