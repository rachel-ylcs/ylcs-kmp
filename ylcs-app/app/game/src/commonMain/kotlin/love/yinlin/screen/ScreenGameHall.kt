package love.yinlin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import love.yinlin.app
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.common.GameItem
import love.yinlin.compose.ui.container.RachelStatefulProvider
import love.yinlin.compose.ui.container.StatefulBox
import love.yinlin.compose.ui.floating.FAB
import love.yinlin.compose.ui.floating.FABAction
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.layout.Pagination
import love.yinlin.compose.ui.layout.PaginationStaggeredGrid
import love.yinlin.cs.*
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.game.GamePublicDetailsWithName

@Stable
class ScreenGameHall(private val game: Game) : Screen() {
    private val provider = RachelStatefulProvider()

    private var page = object : Pagination<GamePublicDetailsWithName, Int, Int>(
        default = Int.MAX_VALUE,
        pageNum = APIConfig.MIN_PAGE_NUM
    ) {
        override fun distinctValue(item: GamePublicDetailsWithName): Int = item.gid
        override fun offset(item: GamePublicDetailsWithName): Int = item.gid
    }

    private val gridState = LazyStaggeredGridState()

    private suspend fun requestNewGames(loading: Boolean) {
        provider.withLoading(loading) {
            page.newData(ApiGameGetGames.requestNull(game, page.default, page.pageNum)!!.o1)
        }
    }

    private suspend fun requestMoreGames() {
        ApiGameGetGames.request(game, page.offset, page.pageNum) { page.moreData(it) }
    }

    private suspend fun deleteGame(gid: Int) {
        ApiGameDeleteGame.request(app.config.userToken, gid) {
            page.items.removeAll { it.gid == gid }
        }.errorTip
    }

    override val title: String = game.title

    override suspend fun initialize() {
        requestNewGames(true)
    }

    @Composable
    override fun Content() {
        StatefulBox(
            provider = provider,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            PaginationStaggeredGrid(
                items = page.items,
                key = { it.gid },
                columns = StaggeredGridCells.Adaptive(Theme.size.cell1),
                state = gridState,
                canRefresh = true,
                canLoading = page.canLoading,
                onRefresh = { requestNewGames(false) },
                onLoading = { requestMoreGames() },
                modifier = Modifier.fillMaxSize(),
                contentPadding = Theme.padding.eValue,
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
                verticalItemSpacing = Theme.padding.e
            ) {
                GameItem(
                    gameDetails = it,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val profile = app.config.userProfile
                        if (profile != null) {
                            if (profile.name == it.name) slot.tip.warning("不能参与自己创建的游戏哦")
                            else if (profile.name in it.winner) slot.tip.warning("不能参与完成过的游戏哦")
                            else if (profile.coin < it.cost) slot.tip.warning("银币不足入场")
                            else navigate(::ScreenPlayGame, it)
                        }
                        else slot.tip.warning("请先登录")
                    }
                ) {
                    if (app.config.userProfile?.hasPrivilegeVIPTopic == true) {
                        LoadingIcon(
                            icon = Icons.Delete,
                            tip = "删除",
                            onClick = {
                                if (slot.confirm.open(content = "删除仅返还奖池内剩余银币")) deleteGame(it.gid)
                            }
                        )
                    }
                }
            }
        }
    }

    override val fab: FAB = object : FAB() {
        private val isScrollTop: Boolean by derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }

        override val action: FABAction = FABAction(
            iconProvider = { if (isScrollTop) Icons.Refresh else Icons.ArrowUpward },
            onClick = {
                if (isScrollTop) requestNewGames(true)
                else gridState.animateScrollToItem(0)
            }
        )
    }
}