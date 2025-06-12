package love.yinlin.ui.screen.world

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
import love.yinlin.AppModel
import love.yinlin.Local
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.Colors
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.Data
import love.yinlin.data.rachel.game.GameRecordWithName
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.extension.Array
import love.yinlin.extension.makeArray
import love.yinlin.extension.to
import love.yinlin.platform.app
import love.yinlin.resources.Res
import love.yinlin.resources.img_not_login
import love.yinlin.resources.img_state_loading
import love.yinlin.resources.img_state_network_error
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.input.RachelText
import love.yinlin.ui.component.layout.*
import love.yinlin.ui.component.screen.CommonSubScreen
import love.yinlin.ui.component.screen.FloatingArgsSheet
import love.yinlin.ui.screen.world.game.GameRecordCard

@Stable
class ScreenGameRecordHistory(model: AppModel) : CommonSubScreen(model) {
    private var state by mutableStateOf(BoxState.EMPTY)

    private val page = object : Pagination<GameRecordWithName, Long, Long>(Long.MAX_VALUE) {
        override fun distinctValue(item: GameRecordWithName): Long = item.rid
        override fun offset(item: GameRecordWithName): Long = item.rid
    }

    private val gridState = LazyStaggeredGridState()

    private suspend fun requestNewGameRecords() {
        if (state != BoxState.LOADING) {
            state = BoxState.LOADING
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
            shadowElevation = ThemeValue.Shadow.Surface,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(ThemeValue.Padding.EqualExtraValue),
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WebImage(
                        uri = remember { record.type.yPath },
                        key = Local.VERSION,
                        contentScale = ContentScale.Crop,
                        circle = true,
                        modifier = Modifier.size(ThemeValue.Size.MediumImage)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
                    ) {
                        RachelText(text = record.name, icon = Icons.Outlined.AccountCircle)
                        RachelText(text = record.ts, icon = Icons.Outlined.Timer)
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
        requestNewGameRecords()
    }

    @Composable
    override fun SubContent(device: Device) {
        StatefulBox(
            state = state,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            PaginationStaggeredGrid(
                items = page.items,
                key = { it.rid },
                columns = StaggeredGridCells.Adaptive(ThemeValue.Size.CardWidth),
                state = gridState,
                canRefresh = true,
                canLoading = page.canLoading,
                onRefresh = { requestNewGameRecords() },
                onLoading = { requestMoreGameRecords() },
                modifier = Modifier.fillMaxSize(),
                contentPadding = ThemeValue.Padding.EqualValue,
                horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                verticalItemSpacing = ThemeValue.Padding.EqualSpace
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

    override val fabCanExpand: Boolean = false

    override val fabIcon: ImageVector get() = if (isScrollTop) Icons.Outlined.Refresh else Icons.Outlined.ArrowUpward

    override suspend fun onFabClick() {
        if (isScrollTop) launch { requestNewGameRecords() }
        else gridState.animateScrollToItem(0)
    }

    private val recordDetailsSheet = object : FloatingArgsSheet<GameRecordWithName>() {
        @Composable
        override fun Content(args: GameRecordWithName) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.SheetValue),
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
            ) {
                val (answers, results) = remember(args) {
                    try {
                        val results = when (val tmp = args.result) {
                            null -> emptyList()
                            is JsonArray -> tmp
                            else -> listOf(tmp)
                        }.map { it.to<GameResult>() }
                        val answers = when (results.size) {
                            0 -> emptyList()
                            else -> args.answer.Array
                        }
                        require(answers.size == results.size)
                        answers to results
                    }
                    catch (_: Throwable) {
                        makeArray { } to emptyList()
                    }
                }

                if (results.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MiniIcon(
                            res = Res.drawable.img_not_login,
                            size = ThemeValue.Size.MediumImage
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
                        verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
                    ) {
                        items(answers.size) { index ->
                            val answer = answers[index]
                            val result = results[index]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MiniIcon(
                                    res = if (result.isCompleted) Res.drawable.img_state_loading else Res.drawable.img_state_network_error,
                                    size = ThemeValue.Size.MediumImage
                                )
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
                                horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RachelText(text = result.reward.toString(), icon = Icons.Outlined.Diamond)
                                RachelText(text = result.rank.toString(), icon = Icons.Outlined.FormatListNumbered)
                            }
                            Space()
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
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