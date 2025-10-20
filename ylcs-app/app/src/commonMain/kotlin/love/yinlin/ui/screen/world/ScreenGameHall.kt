package love.yinlin.ui.screen.world

import androidx.compose.foundation.layout.Arrangement
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
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.ThemeValue
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.rememberDerivedState
import love.yinlin.data.Data
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.game.GamePublicDetailsWithName
import love.yinlin.platform.app
import love.yinlin.ui.component.image.LoadingIcon
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.Pagination
import love.yinlin.ui.component.layout.PaginationStaggeredGrid
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.world.game.GameItem

@Stable
class ScreenGameHall(model: AppModel, val args: Args) : SubScreen<ScreenGameHall.Args>(model) {
    @Stable
    @Serializable
    data class Args(val type: Game)

    private var state by mutableStateOf(BoxState.EMPTY)

    private var page = object : Pagination<GamePublicDetailsWithName, Int, Int>(Int.MAX_VALUE) {
        override fun distinctValue(item: GamePublicDetailsWithName): Int = item.gid
        override fun offset(item: GamePublicDetailsWithName): Int = item.gid
    }

    private val gridState = LazyStaggeredGridState()

    override val title: String = args.type.title

    private suspend fun requestNewGames(loading: Boolean) {
        if (state != BoxState.LOADING) {
            if (loading) state = BoxState.LOADING
            val result = ClientAPI.request(
                route = API.User.Game.GetGames,
                data = API.User.Game.GetGames.Request(
                    type = args.type,
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
            route = API.User.Game.GetGames,
            data = API.User.Game.GetGames.Request(
                type = args.type,
                gid = page.offset,
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

    override suspend fun initialize() {
        requestNewGames(true)
    }

    @Composable
    override fun SubContent(device: Device) {
        StatefulBox(
            state = state,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            val canDelete by rememberDerivedState { app.config.userProfile?.hasPrivilegeVIPTopic == true }

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
                    game = it,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val profile = app.config.userProfile
                        if (profile != null) {
                            if (profile.name == it.name) slot.tip.warning("不能参与自己创建的游戏哦")
                            else if (profile.name in it.winner) slot.tip.warning("不能参与完成过的游戏哦")
                            else if (profile.coin < it.cost) slot.tip.warning("银币不足入场")
                            else {
                                worldPart.currentGame = it
                                navigate<ScreenPlayGame>()
                            }
                        }
                        else slot.tip.warning("请先登录")
                    }
                ) {
                    if (canDelete) {
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
    }

    private val isScrollTop: Boolean by derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }

    override val fabIcon: ImageVector get() = if (isScrollTop) Icons.Outlined.Refresh else Icons.Outlined.ArrowUpward

    override suspend fun onFabClick() {
        if (isScrollTop) launch { requestNewGames(true) }
        else gridState.animateScrollToItem(0)
    }
}