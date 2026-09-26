package love.yinlin.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import love.yinlin.common.BasicWeiboManager
import love.yinlin.common.MessageManager
import love.yinlin.common.MessageType
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.ds.DataSourceInformation
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.container.HorizontalScrollContainer
import love.yinlin.compose.ui.container.RachelStatefulProvider
import love.yinlin.compose.ui.container.StatefulBox
import love.yinlin.compose.ui.container.StatefulStatus
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.layout.PaginationStaggeredGrid

@Stable
class ScreenInformation : BasicScreen() {
    private val provider = RachelStatefulProvider()
    private var currentManager: MessageManager<*> by mutableStateOf(DataSourceInformation.managers[MessageType.Default]!!)
    private var isNavigating by mutableStateOf(false)

    init {
        land(BasicWeiboManager.CommonDownloadDialog)
    }

    private fun flushContent() {
        if (provider.isLoading) provider.status = StatefulStatus.Content
    }

    private suspend fun requestNewData(manager: MessageManager<*>) {
        isNavigating = true
        provider.withLoading { manager.requestNewData(::flushContent) }
        isNavigating = false
    }

    private suspend fun requestMoreData(manager: MessageManager<*>) {
        isNavigating = true
        manager.requestMoreData(::flushContent)
        isNavigating = false
    }

    override suspend fun initialize() {
        requestNewData(currentManager)
    }

    @Composable
    override fun BasicContent() {
        Column(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            val state = rememberLazyListState()

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = Theme.shadow.v7
            ) {
                HorizontalScrollContainer(state = state, modifier = Modifier.fillMaxWidth()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().background(Theme.color.surface),
                        state = state,
                        contentPadding = Theme.padding.value,
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(items = MessageType.entries, key = { it }) { type ->
                            val manager = DataSourceInformation.managers[type]!!
                            val isSelected = currentManager == manager

                            Icon(
                                icon = manager.icon,
                                color = Colors.Unspecified,
                                modifier = Modifier.clip(Theme.shape.v7)
                                    .background(if (isSelected) Theme.color.secondaryContainer.copy(alpha = 0.5f) else Colors.Transparent)
                                    .clickable(enabled = !isNavigating) {
                                        if (currentManager == manager) {
                                            with(manager) { openSettings() }
                                        }
                                        else {
                                            // 防止正在切换
                                            if (!isNavigating) {
                                                currentManager = manager
                                                // 检查是否有数据需要更新
                                                if (manager.items.isEmpty()) {
                                                    launch { requestNewData(manager) }
                                                }
                                            }
                                        }
                                    }.padding(Theme.padding.g2).size(Theme.size.image9)
                            )
                        }
                    }
                }
            }

            StatefulBox(
                provider = provider,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                PaginationStaggeredGrid(
                    items = currentManager.items,
                    key = { it.id },
                    columns = StaggeredGridCells.Adaptive(Theme.size.cell1),
                    state = currentManager.gridState,
                    canRefresh = true,
                    canLoading = currentManager.canLoading,
                    onRefresh = {
                        // 防止正在切换
                        if (!isNavigating) {
                            launch { requestNewData(currentManager) }
                        }
                    },
                    onLoading = {
                        if (!isNavigating) {
                            launch { requestMoreData(currentManager) }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = Theme.padding.eValue,
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
                    verticalItemSpacing = Theme.padding.e
                ) { message ->
                    with(currentManager) {
                        MessageLayout(modifier = Modifier.fillMaxWidth(), message = message)
                    }
                }
            }
        }
    }
}