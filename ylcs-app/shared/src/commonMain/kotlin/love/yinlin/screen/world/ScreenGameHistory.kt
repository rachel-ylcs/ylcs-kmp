package love.yinlin.screen.world

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.api.APIConfig
import love.yinlin.api.ApiGameDeleteGame
import love.yinlin.api.ApiGameGetUserGames
import love.yinlin.api.request
import love.yinlin.api.requestNull
import love.yinlin.app
import love.yinlin.compose.*
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.data.rachel.game.GameDetailsWithName
import love.yinlin.compose.ui.floating.FloatingArgsSheet
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.PauseLoading
import love.yinlin.compose.ui.layout.PaginationArgs
import love.yinlin.compose.ui.layout.PaginationStaggeredGrid
import love.yinlin.compose.ui.layout.BoxState
import love.yinlin.compose.ui.layout.StatefulBox
import love.yinlin.screen.world.game.GameCardQuestionAnswer
import love.yinlin.screen.world.game.GameItem

@Stable
class ScreenGameHistory(manager: ScreenManager) : Screen(manager) {
    private var state by mutableStateOf(BoxState.EMPTY)

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
        if (state != BoxState.LOADING) {
            if (loading) state = BoxState.LOADING
            state = ApiGameGetUserGames.requestNull(app.config.userToken, page.default, page.default1, page.pageNum)?.let {
                if (page.newData(it.o1)) BoxState.CONTENT else BoxState.EMPTY
            } ?: BoxState.NETWORK_ERROR
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

    override val title: String = "我的游戏"

    override suspend fun initialize() {
        requestNewGames(true)
    }

    @Composable
    override fun Content(device: Device) {
        StatefulBox(
            state = state,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            val name = remember { app.config.userProfile?.name ?: "" }

            PauseLoading(gridState)

            PaginationStaggeredGrid(
                items = page.items,
                key = { it.gid },
                columns = StaggeredGridCells.Adaptive(CustomTheme.size.cardWidth),
                state = gridState,
                canRefresh = true,
                canLoading = page.canLoading,
                onRefresh = { requestNewGames(false) },
                onLoading = { requestMoreGames() },
                modifier = Modifier.fillMaxSize(),
                contentPadding = CustomTheme.padding.equalValue,
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.equalSpace),
                verticalItemSpacing = CustomTheme.padding.equalSpace
            ) {
                GameItem(
                    game = remember(it) { it.toPublic(name) },
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { gameDetailsSheet.open(it) },
                ) {
                    LoadingIcon(
                        icon = Icons.Outlined.Delete,
                        tip = "删除",
                        onClick = {
                            if (slot.confirm.openSuspend(content = "删除仅返还奖池内剩余银币")) {
                                deleteGame(it.gid)
                            }
                        },
                        modifier = Modifier.align(alignment = Alignment.End)
                    )
                }
            }
        }
    }

    private val isScrollTop: Boolean by derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }

    override val fabIcon: ImageVector by derivedStateOf { if (isScrollTop) Icons.Outlined.Refresh else Icons.Outlined.ArrowUpward }

    override suspend fun onFabClick() {
        if (isScrollTop) launch { requestNewGames(true) }
        else gridState.animateScrollToItem(0)
    }

    private val gameDetailsSheet = this land object : FloatingArgsSheet<GameDetailsWithName>() {
        @Composable
        override fun Content(args: GameDetailsWithName) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.sheetValue),
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                GameCardQuestionAnswer(args)
            }
        }
    }
}