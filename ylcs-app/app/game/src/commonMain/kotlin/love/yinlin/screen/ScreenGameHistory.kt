package love.yinlin.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import love.yinlin.app
import love.yinlin.common.GameAnswerInfo
import love.yinlin.common.GameMapper
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.common.GameItem
import love.yinlin.compose.ui.container.RachelStatefulProvider
import love.yinlin.compose.ui.container.StatefulBox
import love.yinlin.compose.ui.floating.FAB
import love.yinlin.compose.ui.floating.FABAction
import love.yinlin.compose.ui.floating.SheetContent
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.layout.PaginationArgs
import love.yinlin.compose.ui.layout.PaginationStaggeredGrid
import love.yinlin.cs.*
import love.yinlin.data.rachel.game.GameDetailsWithName

@Stable
class ScreenGameHistory : Screen() {
    private val provider = RachelStatefulProvider()

    private val page = object : PaginationArgs<GameDetailsWithName, Int, Int, Boolean>(
        default = Int.MAX_VALUE,
        default1 = false,
        pageNum = APIConfig.MIN_PAGE_NUM
    ) {
        override fun distinctValue(item: GameDetailsWithName): Int = item.gid
        override fun offset(item: GameDetailsWithName): Int = item.gid
        override fun arg1(item: GameDetailsWithName): Boolean = item.isCompleted
    }

    private val gridState = LazyStaggeredGridState()

    private suspend fun requestNewGames(loading: Boolean) {
        provider.withLoading(loading) {
            page.newData(ApiGameGetUserGames.requestNull(app.config.userToken, page.default, page.default1, page.pageNum)!!.o1)
        }
    }

    private suspend fun requestMoreGames() {
        ApiGameGetUserGames.request(app.config.userToken, page.offset, page.arg1, page.pageNum) {
            page.moreData(it)
        }
    }

    private suspend fun deleteGame(gid: Int) {
        ApiGameDeleteGame.request(app.config.userToken, gid) {
            page.items.removeAll { it.gid == gid }
        }.errorTip
    }

    override val title: String = "我创建的游戏"

    override suspend fun initialize() {
        requestNewGames(true)
    }

    @Composable
    override fun Content() {
        StatefulBox(
            provider = provider,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            val name = app.config.userProfile?.name ?: ""

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
                    gameDetails = remember(it, name) { it.toPublic(name) },
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { gameDetailsSheet.open(it) },
                ) {
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

    private val gameDetailsSheet = this land object : SheetContent<GameDetailsWithName>() {
        @Composable
        override fun Content(args: GameDetailsWithName) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
            ) {
                GameMapper.cast<GameAnswerInfo>(args.type)?.apply { GameAnswerInfoContent(args) }
            }
        }
    }
}