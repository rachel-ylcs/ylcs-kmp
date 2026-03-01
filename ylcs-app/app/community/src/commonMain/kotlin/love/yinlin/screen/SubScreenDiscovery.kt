package love.yinlin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.app
import love.yinlin.common.DataSourceDiscovery
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.screen.NavigationScreen
import love.yinlin.compose.screen.SubScreen
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.container.RachelStatefulProvider
import love.yinlin.compose.ui.container.StatefulBox
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.floating.FAB
import love.yinlin.compose.ui.floating.FABAction
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.layout.PaginationStaggeredGrid
import love.yinlin.compose.ui.navigation.TabBar
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.cs.*
import love.yinlin.data.rachel.discovery.DiscoveryItem
import love.yinlin.data.rachel.topic.Comment
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.extension.DateEx

@Stable
class SubScreenDiscovery(parent: NavigationScreen) : SubScreen(parent) {
    private val provider = RachelStatefulProvider()
    private val gridState = LazyStaggeredGridState()

    private suspend fun requestNewData(loading: Boolean) {
        provider.withLoading(loading) {
            val page = DataSourceDiscovery.page
            val result = when (val section = DataSourceDiscovery.currentSection) {
                DiscoveryItem.LatestTopic.id -> ApiTopicGetLatestTopics.requestNull(page.default, page.pageNum)
                DiscoveryItem.LatestComment.id -> ApiTopicGetLatestTopicsByComment.requestNull(page.default, page.pageNum)
                DiscoveryItem.Hot.id -> ApiTopicGetHotTopics.requestNull(page.default1, page.default, page.pageNum)
                else -> ApiTopicGetSectionTopics.requestNull(section, page.default, page.pageNum)
            }!!
            gridState.requestScrollToItem(0)
            page.newData(result.o1)
        }
    }

    private suspend fun requestMoreData() {
        val page = DataSourceDiscovery.page
        when (val section = DataSourceDiscovery.currentSection) {
            DiscoveryItem.LatestTopic.id -> ApiTopicGetLatestTopics.requestNull(page.offset, page.pageNum)
            DiscoveryItem.LatestComment.id -> ApiTopicGetLatestTopicsByComment.requestNull(page.offset, page.pageNum)
            DiscoveryItem.Hot.id -> ApiTopicGetHotTopics.requestNull(page.arg1, page.offset, page.pageNum)
            else -> ApiTopicGetSectionTopics.requestNull(section, page.offset, page.pageNum)
        }?.let { page.moreData(it.o1) }
    }

    override suspend fun initialize() {
        requestNewData(true)
    }

    @Composable
    private fun TopicCard(topic: Topic, modifier: Modifier = Modifier) {
        Surface(
            modifier = modifier,
            shape = Theme.shape.v3,
            shadowElevation = Theme.shadow.v3,
            onClick = { navigate(::ScreenTopic, topic) }
        ) {
            Column(modifier = Modifier.fillMaxWidth().heightIn(min = Theme.size.cell4 * 0.777777f)) {
                topic.picPath?.url?.let {
                    WebImage(
                        uri = it,
                        modifier = Modifier.fillMaxWidth().height(Theme.size.cell4 * 1.333333f),
                        contentScale = ContentScale.Crop
                    )
                }
                Text(
                    text = topic.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.value)
                )
                Box(Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(Theme.padding.value),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
                ) {
                    WebImage(
                        uri = topic.avatarPath.url,
                        key = remember { DateEx.TodayString },
                        contentScale = ContentScale.Crop,
                        circle = true,
                        modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                        onClick = { navigate(::ScreenUserCard, topic.uid) }
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                    ) {
                        SimpleEllipsisText(
                            text = topic.name,
                            style = Theme.typography.v8.bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextIconAdapter(modifier = Modifier.weight(1f)) { idIcon, idText ->
                                Icon(icon = Icons.Comment, modifier = Modifier.idIcon())
                                SimpleClipText(
                                    text = topic.commentNum.toString(),
                                    style = Theme.typography.v8,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.idText()
                                )
                            }
                            TextIconAdapter(modifier = Modifier.weight(1f)) { idIcon, idText ->
                                Icon(icon = Icons.Paid, modifier = Modifier.idIcon())
                                SimpleClipText(
                                    text = topic.coinNum.toString(),
                                    style = Theme.typography.v8,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.idText()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun Content() {
        Column(modifier = Modifier.fillMaxSize()) {
            val immersivePadding = LocalImmersivePadding.current

            Surface(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = immersivePadding.withoutBottom,
                shadowElevation = Theme.shadow.v3
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabBar(
                        size = DiscoveryItem.entries.size,
                        index = DataSourceDiscovery.currentPage,
                        onNavigate = {
                            DataSourceDiscovery.currentPage = it
                            launch { requestNewData(true) }
                        },
                        titleProvider = { Comment.Section.sectionName(DiscoveryItem.entries[it].id) },
                        iconProvider = { DiscoveryItem.entries[it].icon },
                        modifier = Modifier.weight(1f)
                    )
                    ActionScope.Right.Container(modifier = Modifier.padding(end = Theme.padding.e)) {
                        Icon(icon = Icons.Edit, tip = "发表", onClick = {
                            if (app.config.userProfile != null) navigate(::ScreenAddTopic)
                            else slot.tip.warning("请先登录")
                        })
                    }
                }
            }

            StatefulBox(
                provider = provider,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(immersivePadding.withoutTop)
            ) {
                PaginationStaggeredGrid(
                    items = DataSourceDiscovery.page.items,
                    key = { it.tid },
                    columns = StaggeredGridCells.Adaptive(Theme.size.cell4),
                    state = gridState,
                    canRefresh = true,
                    canLoading = DataSourceDiscovery.page.canLoading,
                    onRefresh = { requestNewData(false) },
                    onLoading = ::requestMoreData,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = Theme.padding.eValue,
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
                    verticalItemSpacing = Theme.padding.e
                ) { topic ->
                    TopicCard(topic = topic, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    override val fab: FAB = object : FAB() {
        private val isScrollTop by derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }

        override val action: FABAction = FABAction(
            iconProvider = { if (isScrollTop) Icons.Refresh else Icons.ArrowUpward },
            onClick = {
                if (isScrollTop) requestNewData(true)
                else gridState.animateScrollToItem(0)
            }
        )
    }
}