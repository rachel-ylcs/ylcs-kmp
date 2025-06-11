package love.yinlin.ui.screen.world

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.util.fastForEach
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.Local
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.Device
import love.yinlin.common.ExtraIcons
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.Data
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.game.GamePublicDetailsWithName
import love.yinlin.platform.app
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.input.RachelText
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.Pagination
import love.yinlin.ui.component.layout.PaginationGrid
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.screen.FABAction
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.community.BoxText
import love.yinlin.ui.screen.world.game.GameCardInfo

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
        game: GamePublicDetailsWithName,
        modifier: Modifier = Modifier,
        onClick: () -> Unit,
    ) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = ThemeValue.Shadow.Surface,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(ThemeValue.Padding.ExtraValue),
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WebImage(
                        uri = remember { args.type.yPath },
                        key = Local.VERSION,
                        contentScale = ContentScale.Crop,
                        circle = true,
                        modifier = Modifier.size(ThemeValue.Size.MediumImage)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
                    ) {
                        RachelText(text = game.name, icon = Icons.Outlined.AccountCircle)
                        RachelText(text = game.ts, icon = Icons.Outlined.Timer)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            RachelText(
                                text = game.reward.toString(),
                                icon = Icons.Outlined.Diamond,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium
                            )
                            RachelText(
                                text = game.num.toString(),
                                icon = Icons.Outlined.FormatListNumbered,
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.labelMedium
                            )
                            RachelText(
                                text = game.cost.toString(),
                                icon = Icons.Outlined.Paid,
                                color = MaterialTheme.colorScheme.tertiary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                Text(
                    text = game.title,
                    modifier = Modifier.fillMaxWidth()
                )
                GameCardInfo(game = game)
                if (game.winner.isNotEmpty()) {
                    RachelText(text = "赢家", icon = Icons.Outlined.MilitaryTech)
                    FlowRow(modifier = Modifier.fillMaxWidth()) {
                        game.winner.fastForEach { winner ->
                            BoxText(text = winner.toString(), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
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
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val profile = app.config.userProfile
                        if (profile != null) {
                            if (profile.name == it.name) slot.tip.warning("不能参与自己创建的游戏哦")
                            else if (profile.name in it.winner) slot.tip.warning("不能参与完成过的游戏哦")
                            else if (profile.coin < it.cost) slot.tip.warning("银币不足入场")
                            else navigate(ScreenPlayGame.Args(it.type, it.gid))
                        }
                        else slot.tip.warning("请先登录")
                    }
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