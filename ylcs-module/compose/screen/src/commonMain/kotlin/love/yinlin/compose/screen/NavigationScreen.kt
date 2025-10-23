package love.yinlin.compose.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import love.yinlin.compose.Device
import love.yinlin.compose.LaunchFlag
import love.yinlin.compose.LocalAnimationSpeed
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.ui.floating.FABLayout
import kotlin.reflect.KClass

@Stable
abstract class NavigationScreen<A>(manager: ScreenManager) : BasicScreen<A>(manager) {
    @Stable
    data class SubScreenInfo(
        val screen: SubScreen,
        val clz: KClass<out SubScreen>,
        val flag: LaunchFlag = LaunchFlag()
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
        val animationSpeed = LocalAnimationSpeed.current
        Wrapper(LocalDevice.current, pageIndex) { device ->
            AnimatedContent(
                targetState = pageIndex,
                transitionSpec = {
                    val enterAnimation = slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(
                            durationMillis = animationSpeed,
                            easing = FastOutSlowInEasing
                        )
                    )
                    val exitAnimation = slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(
                            durationMillis = animationSpeed,
                            easing = FastOutSlowInEasing
                        )
                    )
                    enterAnimation.togetherWith(exitAnimation)
                }
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
                info.flag(
                    update = { launch { info.screen.initialize(true) } },
                    init = { launch { info.screen.initialize(false) } }
                )
            }
        }
    }

    @Composable
    override fun Floating() = currentScreen.Floating()
}

typealias CommonNavigationScreen = NavigationScreen<Unit>