package love.yinlin.ui.screen.world

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.Device
import love.yinlin.common.ExtraIcons
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.Data
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.game.GamePublicDetails
import love.yinlin.platform.app
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.Pagination
import love.yinlin.ui.component.layout.PaginationGrid
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.screen.FABAction
import love.yinlin.ui.component.screen.SubScreen

@Stable
class ScreenGameHall(model: AppModel, val args: Args) : SubScreen<ScreenGameHall.Args>(model) {
    @Stable
    @Serializable
    data class Args(val type: Game)

    private var state by mutableStateOf(BoxState.EMPTY)

    private var page = object : Pagination<GamePublicDetails, Int, Int>(Int.MAX_VALUE) {
        override fun distinctValue(item: GamePublicDetails): Int = item.gid
        override fun offset(item: GamePublicDetails): Int = item.gid
    }

    private val gridState = LazyGridState()

    override val title: String = "${args.type.title} - 大厅"

    private suspend fun requestNewGames() {
        if (state != BoxState.LOADING) {
            state = BoxState.LOADING
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

    @Composable
    private fun GameItem(
        game: GamePublicDetails,
        modifier: Modifier = Modifier
    ) {

    }

    override suspend fun initialize() {
        requestNewGames()
    }

    @Composable
    override fun SubContent(device: Device) {
        StatefulBox(
            state = state,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            PaginationGrid(
                items = page.items,
                key = { it.gid },
                columns = GridCells.Adaptive(ThemeValue.Size.CardWidth),
                state = gridState,
                canRefresh = true,
                canLoading = page.canLoading,
                onRefresh = { requestNewGames() },
                onLoading = { requestMoreGames() },
                modifier = Modifier.fillMaxSize(),
                contentPadding = ThemeValue.Padding.EqualValue,
                horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace)
            ) {
                GameItem(
                    game = it,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    override val fabCanExpand: Boolean by derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }

    override val fabIcon: ImageVector? by derivedStateOf { if (fabCanExpand) Icons.Outlined.Add else Icons.Outlined.ArrowUpward }

    override val fabMenus: Array<FABAction> = arrayOf(
        FABAction(Icons.Outlined.Edit) {
            if (app.config.userProfile != null) {
                pop()
                navigate(ScreenCreateGame.Args(args.type))
            }
            else slot.tip.warning("请先登录")
        },
        FABAction(ExtraIcons.RewardCup) {
            navigate(ScreenGameRanking.Args(args.type))
        },
        FABAction(Icons.Outlined.Refresh) {
            launch { requestNewGames() }
        }
    )

    override suspend fun onFabClick() {
        gridState.animateScrollToItem(0)
    }
}