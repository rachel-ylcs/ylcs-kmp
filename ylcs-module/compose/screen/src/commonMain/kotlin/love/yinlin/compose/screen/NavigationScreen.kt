package love.yinlin.compose.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import love.yinlin.compose.LaunchFlag

@Stable
abstract class NavigationScreen : BasicScreen() {
    /**
     * 装饰器
     *
     * 使用装饰器包裹导航切换组件并调用实际内容 content
     */
    @Composable
    protected abstract fun DecorateContent(index: Int, content: @Composable () -> Unit)

    @Stable
    internal class SubScreenInfo(val screen: SubScreen, val flag: LaunchFlag = LaunchFlag())

    private val subScreenList = mutableStateListOf<SubScreenInfo>()

    /**
     * 创建子页面
     */
    fun <S : SubScreen> create(factory: (BasicScreen) -> S) { subScreenList += SubScreenInfo(screen = factory(this)) }

    private val pagerState = PagerState { subScreenList.size }

    /**
     * 当前页索引
     */
    var pageIndex: Int get() = pagerState.currentPage
        set(value) {
            launch { pagerState.scrollToPage(value) }
        }

    @Composable
    final override fun BasicContent() {
        DecorateContent(pagerState.currentPage) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 0,
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize()
            ) { index ->
                Box(modifier = Modifier.fillMaxSize()) {
                    subScreenList.getOrNull(index)?.screen?.let { subScreen ->
                        subScreen.Content()

                        subScreen.fab.Land()
                    }
                }
            }

            LaunchedEffect(pageIndex) {
                subScreenList.getOrNull(pageIndex)?.let { info ->
                    info.flag(
                        update = {
                            launch { info.screen.update() }
                        },
                        init = {
                            launch { info.screen.initialize() }
                        }
                    )
                }
            }
        }
    }

    @Composable
    final override fun Floating() {
        subScreenList.getOrNull(pagerState.currentPage)?.screen?.SubComposedFloating()
    }
}