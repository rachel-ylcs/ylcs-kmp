package love.yinlin.compose.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.compose.Device
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.ui.floating.FABAction
import kotlin.reflect.KClass

@Stable
abstract class NavigationScreen<A>(manager: ScreenManager) : BasicScreen<A>(manager) {
    @Stable
    data class SubScreenInfo(
        val screen: SubScreen,
        val clz: KClass<out SubScreen>
    )

    abstract val subs: List<SubScreenInfo>

    inline fun <reified S : SubScreen> sub(factory: (BasicScreen<*>) -> S): SubScreenInfo = SubScreenInfo(factory(this), S::class)

    inline fun <reified S : SubScreen> get(): S = subs.first { it.clz == S::class }.screen as S

    var pageIndex by mutableIntStateOf(0)

    private val currentScreen: SubScreen get() = subs[pageIndex].screen

    @Composable
    protected abstract fun Wrapper(device: Device, index: Int, content: @Composable (Device) -> Unit)

    @Composable
    override fun BasicContent() {
        Wrapper(LocalDevice.current, pageIndex) { device ->
            AnimatedContent(targetState = pageIndex) { index ->
                Box(modifier = Modifier.fillMaxSize()) {
                    subs[index].screen.Content(device)
                }
            }

            LaunchedEffect(pageIndex) {
                val subScreen = currentScreen
                subScreen.firstLoad(
                    update = { launch { subScreen.initialize(true) } },
                    init = { launch { subScreen.initialize(false) } }
                )
            }
        }
    }

    override val fabIcon: ImageVector? get() = currentScreen.fabIcon
    override val fabCanExpand: Boolean get() = currentScreen.fabCanExpand
    override val fabMenus: Array<FABAction> get() = currentScreen.fabMenus
    override suspend fun onFabClick() = currentScreen.onFabClick()

    @Composable
    override fun Floating() = currentScreen.Floating()
}

typealias CommonNavigationScreen = NavigationScreen<Unit>