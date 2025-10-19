package love.yinlin.ui.screen.world

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
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.Data
import love.yinlin.data.rachel.game.GameDetailsWithName
import love.yinlin.platform.app
import love.yinlin.ui.component.image.LoadingIcon
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.PaginationArgs
import love.yinlin.ui.component.layout.PaginationStaggeredGrid
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.screen.CommonSubScreen
import love.yinlin.ui.component.screen.FloatingArgsSheet
import love.yinlin.ui.screen.world.game.GameCardQuestionAnswer
import love.yinlin.ui.screen.world.game.GameItem

@Stable
class ScreenGameHistory(model: AppModel) : CommonSubScreen(model) {
    private var state by mutableStateOf(BoxState.EMPTY)

    private val page = object : PaginationArgs<GameDetailsWithName, Int, Int, Boolean>(Int.MAX_VALUE, false) {
        override fun distinctValue(item: GameDetailsWithName): Int = item.gid
        override fun offset(item: GameDetailsWithName): Int = item.gid
        override fun arg1(item: GameDetailsWithName): Boolean = item.isCompleted
    }

    private val gridState = LazyStaggeredGridState()

    private suspend fun requestNewGames(loading: Boolean) {
        if (state != BoxState.LOADING) {
            if (loading) state = BoxState.LOADING
            val result = ClientAPI.request(
                route = API.User.Game.GetUserGames,
                data = API.User.Game.GetUserGames.Request(
                    token = app.config.userToken,
                    num = page.pageNum
                )
            )
            state = if (result is Data.Success) {
                if (page.newData(result.data)) BoxState.CONTENT else BoxState.EMPTY
            } else BoxState.NETWORK_ERROR
        }
    }

    private suspend fun requestMoreGames() {
        val result = ClientAPI.request(
            route = API.User.Game.GetUserGames,
            data = API.User.Game.GetUserGames.Request(
                token = app.config.userToken,
                gid = page.offset,
                isCompleted = page.arg1,
                num = page.pageNum
            )
        )
        if (result is Data.Success) page.moreData(result.data)
    }

    private suspend fun deleteGame(gid: Int) {
        val result = ClientAPI.request(
            route = API.User.Game.DeleteGame,
            data = API.User.Game.DeleteGame.Request(
                token = app.config.userToken,
                gid = gid
            )
        )
        when (result) {
            is Data.Success -> page.items.removeAll { it.gid == gid }
            is Data.Failure -> slot.tip.error(result.message)
        }
    }

    override val title: String = "我的游戏"

    override suspend fun initialize() {
        requestNewGames(true)
    }

    @Composable
    override fun SubContent(device: Device) {
        StatefulBox(
            state = state,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            val name = remember { app.config.userProfile?.name ?: "" }

            PaginationStaggeredGrid(
                items = page.items,
                key = { it.gid },
                columns = StaggeredGridCells.Adaptive(ThemeValue.Size.CardWidth),
                state = gridState,
                canRefresh = true,
                canLoading = page.canLoading,
                onRefresh = { requestNewGames(false) },
                onLoading = { requestMoreGames() },
                modifier = Modifier.fillMaxSize(),
                contentPadding = ThemeValue.Padding.EqualValue,
                horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                verticalItemSpacing = ThemeValue.Padding.EqualSpace
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

    override val fabIcon: ImageVector get() = if (isScrollTop) Icons.Outlined.Refresh else Icons.Outlined.ArrowUpward

    override suspend fun onFabClick() {
        if (isScrollTop) launch { requestNewGames(true) }
        else gridState.animateScrollToItem(0)
    }

    private val gameDetailsSheet = object : FloatingArgsSheet<GameDetailsWithName>() {
        @Composable
        override fun Content(args: GameDetailsWithName) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.SheetValue),
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
            ) {
                GameCardQuestionAnswer(args)
            }
        }
    }

    @Composable
    override fun Floating() {
        gameDetailsSheet.Land()
    }
}