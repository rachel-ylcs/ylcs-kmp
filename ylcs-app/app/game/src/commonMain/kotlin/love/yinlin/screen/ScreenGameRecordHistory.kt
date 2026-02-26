package love.yinlin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import kotlinx.serialization.json.JsonArray
import love.yinlin.Local
import love.yinlin.app
import love.yinlin.common.GameMapper
import love.yinlin.common.GameRecordInfo
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.common.TopPager
import love.yinlin.compose.ui.container.RachelStatefulProvider
import love.yinlin.compose.ui.container.StatefulBox
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.floating.FAB
import love.yinlin.compose.ui.floating.FABAction
import love.yinlin.compose.ui.floating.SheetContent
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.layout.Pagination
import love.yinlin.compose.ui.layout.PaginationStaggeredGrid
import love.yinlin.compose.ui.layout.Space
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.cs.*
import love.yinlin.data.rachel.game.GameRecordWithName
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.extension.Array
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.makeArray
import love.yinlin.extension.to

@Stable
class ScreenGameRecordHistory : Screen() {
    private val provider = RachelStatefulProvider()

    private val page = object : Pagination<GameRecordWithName, Long, Long>(
        default = Long.MAX_VALUE,
        pageNum = APIConfig.MIN_PAGE_NUM
    ) {
        override fun distinctValue(item: GameRecordWithName): Long = item.rid
        override fun offset(item: GameRecordWithName): Long = item.rid
    }

    private val gridState = LazyStaggeredGridState()

    private suspend fun requestNewGameRecords(loading: Boolean) {
        provider.withLoading(loading) {
            page.newData(ApiGameGetUserGameRecords.requestNull(app.config.userToken, page.default, page.pageNum)!!.o1)
        }
    }

    private suspend fun requestMoreGameRecords() {
        ApiGameGetUserGameRecords.request(app.config.userToken, page.offset, page.pageNum) {
            page.moreData(it)
        }
    }

    override val title: String = "我的游戏记录"

    override suspend fun initialize() {
        requestNewGameRecords(true)
    }

    @Composable
    private fun GameRecordItem(record: GameRecordWithName, modifier: Modifier = Modifier, onClick: () -> Unit) {
        Surface(
            modifier = modifier,
            shape = Theme.shape.v3,
            contentPadding = Theme.padding.value,
            shadowElevation = Theme.shadow.v3,
            onClick = onClick
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v10)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
                ) {
                    WebImage(
                        uri = record.type.logo.url,
                        key = Local.info.version,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxHeight().aspectRatio(1f).clip(Theme.shape.v7)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v10)
                    ) {
                        SimpleEllipsisText(text = record.type.title, style = Theme.typography.v7.bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextIconAdapter { idIcon, idText ->
                                Icon(icon = Icons.AccountCircle, modifier = Modifier.idIcon())
                                SimpleEllipsisText(text = record.name, modifier = Modifier.idText())
                            }
                            SimpleEllipsisText(text = record.ts, color = LocalColorVariant.current, style = Theme.typography.v8)
                        }
                    }
                }
                Text(text = record.title, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    @Composable
    override fun Content() {
        StatefulBox(
            provider = provider,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            PaginationStaggeredGrid(
                items = page.items,
                key = { it.rid },
                columns = StaggeredGridCells.Adaptive(Theme.size.cell1),
                state = gridState,
                canRefresh = true,
                canLoading = page.canLoading,
                onRefresh = { requestNewGameRecords(false) },
                onLoading = ::requestMoreGameRecords,
                modifier = Modifier.fillMaxSize(),
                contentPadding = Theme.padding.eValue,
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
                verticalItemSpacing = Theme.padding.e
            ) {
                GameRecordItem(
                    record = it,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { recordDetailsSheet.open(it) }
                )
            }
        }
    }

    override val fab: FAB = object : FAB() {
        private val isScrollTop: Boolean by derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }

        override val action: FABAction = FABAction(
            iconProvider = { if (isScrollTop) Icons.Refresh else Icons.ArrowUpward },
            onClick = {
                if (isScrollTop) requestNewGameRecords(true)
                else gridState.animateScrollToItem(0)
            }
        )
    }

    private val recordDetailsSheet = this land object : SheetContent<GameRecordWithName>() {
        @Composable
        override fun Content(args: GameRecordWithName) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
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

                if (results.isEmpty() || answers.isEmpty()) {
                    ThemeContainer(Colors.Yellow5) {
                        TextIconAdapter { idIcon, idText ->
                            Icon(icon = Icons.Lightbulb, modifier = Modifier.idIcon())
                            SimpleEllipsisText(text = "无记录数据", style = Theme.typography.v6.bold, modifier = Modifier.idText())
                        }
                    }
                    Space()
                    SimpleEllipsisText(text = "你可能未完成对局或提前离开", color = LocalColorVariant.current)
                }
                else {
                    var currentIndex by rememberValueState(0)
                    val answer = answers[currentIndex]
                    val result = results[currentIndex]

                    if (answers.size > 1) {
                        TopPager(
                            currentIndex = currentIndex,
                            name = "",
                            onIncrease = { if (currentIndex < answers.size - 1) ++currentIndex },
                            onDecrease = { if (currentIndex > 0) --currentIndex },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ThemeContainer(if (result.isCompleted) Colors.Green5 else Colors.Red5) {
                            TextIconAdapter { idIcon, idText ->
                                Icon(icon = if (result.isCompleted) Icons.Check else Icons.Error, modifier = Modifier.idIcon())
                                SimpleEllipsisText(text = if (result.isCompleted) "成功" else "失败", style = Theme.typography.v7.bold, modifier = Modifier.idText())
                            }
                        }
                        TextIconAdapter { idIcon, idText ->
                            Icon(icon = Icons.Diamond, modifier = Modifier.idIcon())
                            SimpleEllipsisText(text = "奖励 ${result.reward}", modifier = Modifier.idText())
                        }
                        TextIconAdapter { idIcon, idText ->
                            Icon(icon = Icons.FormatListNumbered, modifier = Modifier.idIcon())
                            SimpleEllipsisText(text = "名次 ${result.rank}", modifier = Modifier.idText())
                        }
                    }

                    Space()

                    val data = remember(currentIndex) { GameRecordInfo.Data(answer, result.info) }
                    GameMapper.cast<GameRecordInfo>(args.type)?.apply { GameRecordInfoContent(data) }
                }
            }
        }
    }
}