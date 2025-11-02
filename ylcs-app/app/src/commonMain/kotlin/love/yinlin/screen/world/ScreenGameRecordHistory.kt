package love.yinlin.screen.world

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
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
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.serialization.json.JsonArray
import love.yinlin.Local
import love.yinlin.api.API
import love.yinlin.api.APIConfig
import love.yinlin.api.ClientAPI
import love.yinlin.app
import love.yinlin.compose.Device
import love.yinlin.compose.*
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.data.Data
import love.yinlin.data.rachel.game.GameRecordWithName
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.extension.Array
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.makeArray
import love.yinlin.extension.to
import love.yinlin.resources.img_not_login
import love.yinlin.compose.ui.image.MiniIcon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.NormalText
import love.yinlin.compose.ui.layout.*
import love.yinlin.compose.ui.floating.FloatingArgsSheet
import love.yinlin.compose.ui.image.PauseLoading
import love.yinlin.compose.ui.layout.BoxState
import love.yinlin.compose.ui.layout.Space
import love.yinlin.compose.ui.layout.StatefulBox
import love.yinlin.compose.ui.layout.StatusBox
import love.yinlin.screen.world.game.GameRecordCard

@Stable
class ScreenGameRecordHistory(manager: ScreenManager) : Screen(manager) {
    private var state by mutableStateOf(BoxState.EMPTY)

    private val page = object : Pagination<GameRecordWithName, Long, Long>(
        default = Long.MAX_VALUE,
        pageNum = APIConfig.MIN_PAGE_NUM
    ) {
        override fun distinctValue(item: GameRecordWithName): Long = item.rid
        override fun offset(item: GameRecordWithName): Long = item.rid
    }

    private val gridState = LazyStaggeredGridState()

    private suspend fun requestNewGameRecords(loading: Boolean) {
        if (state != BoxState.LOADING) {
            if (loading) state = BoxState.LOADING
            val result = ClientAPI.request(
                route = API.User.Game.GetUserGameRecords,
                data = API.User.Game.GetUserGameRecords.Request(
                    token = app.config.userToken,
                    num = page.pageNum
                )
            )
            state = if (result is Data.Success) {
                if (page.newData(result.data)) BoxState.CONTENT else BoxState.EMPTY
            } else BoxState.NETWORK_ERROR
        }
    }

    private suspend fun requestMoreGameRecords() {
        val result = ClientAPI.request(
            route = API.User.Game.GetUserGameRecords,
            data = API.User.Game.GetUserGameRecords.Request(
                token = app.config.userToken,
                rid = page.offset,
                num = page.pageNum
            )
        )
        if (result is Data.Success) page.moreData(result.data)
    }

    @Composable
    private fun GameRecordItem(
        record: GameRecordWithName,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = CustomTheme.shadow.surface,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(CustomTheme.padding.equalExtraValue),
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WebImage(
                        uri = remember { record.type.yPath },
                        key = Local.info.version,
                        contentScale = ContentScale.Crop,
                        circle = true,
                        modifier = Modifier.size(CustomTheme.size.mediumImage)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
                    ) {
                        NormalText(text = record.name, icon = Icons.Outlined.AccountCircle)
                        NormalText(text = record.ts, icon = Icons.Outlined.Timer)
                    }
                }

                Text(
                    text = record.title,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    override val title: String = "战绩"

    override suspend fun initialize() {
        requestNewGameRecords(true)
    }

    @Composable
    override fun Content(device: Device) {
        StatefulBox(
            state = state,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            PauseLoading(gridState)

            PaginationStaggeredGrid(
                items = page.items,
                key = { it.rid },
                columns = StaggeredGridCells.Adaptive(CustomTheme.size.cardWidth),
                state = gridState,
                canRefresh = true,
                canLoading = page.canLoading,
                onRefresh = { requestNewGameRecords(false) },
                onLoading = { requestMoreGameRecords() },
                modifier = Modifier.fillMaxSize(),
                contentPadding = CustomTheme.padding.equalValue,
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.equalSpace),
                verticalItemSpacing = CustomTheme.padding.equalSpace
            ) {
                GameRecordItem(
                    record = it,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { recordDetailsSheet.open(it) }
                )
            }
        }
    }

    private val isScrollTop: Boolean by derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }

    override val fabIcon: ImageVector by derivedStateOf { if (isScrollTop) Icons.Outlined.Refresh else Icons.Outlined.ArrowUpward }

    override suspend fun onFabClick() {
        if (isScrollTop) launch { requestNewGameRecords(true) }
        else gridState.animateScrollToItem(0)
    }

    private val recordDetailsSheet = object : FloatingArgsSheet<GameRecordWithName>() {
        @Composable
        override fun Content(args: GameRecordWithName) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.sheetValue),
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                val (answers, results) = remember(args) {
                    catchingDefault({ makeArray { } to emptyList() }) {
                        val (results, answers) = when (val tmp = args.result) {
                            null -> emptyList<GameResult>() to emptyList()
                            is JsonArray -> tmp.map { it.to<GameResult>() } to args.answer.Array
                            else -> listOf(tmp.to<GameResult>()) to listOf(args.answer!!)
                        }
                        require(answers.size == results.size)
                        answers to results
                    }
                }

                if (results.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MiniIcon(
                            res = love.yinlin.resources.Res.drawable.img_not_login,
                            size = CustomTheme.size.mediumImage
                        )
                        Text(
                            text = "无数据",
                            style = MaterialTheme.typography.displayMedium,
                            color = Colors.Yellow4,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
                    ) {
                        items(answers.size) { index ->
                            val answer = answers[index]
                            val result = results[index]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusBox(ok = result.isCompleted, size = CustomTheme.size.mediumImage)
                                Text(
                                    text = if (result.isCompleted) "成功" else "失败",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = if (result.isCompleted) Colors.Green4 else Colors.Red4,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Space()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                NormalText(text = result.reward.toString(), icon = Icons.Outlined.Diamond)
                                NormalText(text = result.rank.toString(), icon = Icons.Outlined.FormatListNumbered)
                            }
                            Space()
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
                            ) {
                                GameRecordCard(
                                    type = args.type,
                                    answer = answer,
                                    info = result.info
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun Floating() {
        recordDetailsSheet.Land()
    }
}