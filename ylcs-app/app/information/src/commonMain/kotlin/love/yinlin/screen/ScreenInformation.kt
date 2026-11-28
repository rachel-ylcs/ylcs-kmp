package love.yinlin.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import love.yinlin.common.APILevel
import love.yinlin.common.MessageManager
import love.yinlin.common.MessageType
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.ds.DataSourceInformation
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.layout.HorizontalScrollContainer
import love.yinlin.compose.ui.container.RachelStatefulProvider
import love.yinlin.compose.ui.container.StatefulBox
import love.yinlin.compose.ui.container.StatefulStatus
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.layout.PaginationStaggeredGrid
import love.yinlin.compose.ui.node.condition
import love.yinlin.extension.cleaning

@Stable
class ScreenInformation : BasicScreen() {
    private val errorRequestText = "可能触发风控啦, 再试试呗~"
    private val errorRequestHandlerText = "重试"

    private val provider = RachelStatefulProvider(
        emptyText = errorRequestText,
        emptyHandler = ::requestNewData,
        emptyHandlerText = errorRequestHandlerText,
        networkErrorText = errorRequestText,
        networkErrorHandler = ::requestNewData,
        networkErrorHandlerText = errorRequestHandlerText
    )

    private var currentManager: MessageManager<*> by mutableStateOf(DataSourceInformation.manager(MessageType.Default))
    private var isNavigating by mutableStateOf(false)

    init {
        land(DataSourceInformation.CommonDownloadDialog)
        land(DataSourceInformation.ChoiceDialog)
    }

    private fun flushContent() {
        if (provider.isLoading) provider.status = StatefulStatus.Content
    }

    private fun onNavigate(manager: MessageManager<*>) {
        // 防止正在切换
        if (currentManager != manager && !isNavigating) {
            currentManager = manager
            // 检查是否有数据需要更新
            if (manager.items.isEmpty()) requestNewData(manager)
            else provider.status = StatefulStatus.Content
        }
    }

    private fun requestNewData(manager: MessageManager<*> = currentManager) {
        if (!isNavigating) {
            launch {
                if (!isNavigating) {
                    isNavigating = true
                    cleaning({
                        provider.withLoading { manager.requestNewData(::flushContent) }
                    }) { isNavigating = false }
                }
            }
        }
    }

    private fun requestMoreData(manager: MessageManager<*> = currentManager) {
        if (!isNavigating) {
            launch {
                if (!isNavigating) {
                    isNavigating = true
                    cleaning({
                        manager.requestMoreData()
                    }) { isNavigating = false }
                }
            }
        }
    }

    override fun initialize() {
        requestNewData()
    }

    @Composable
    override fun BasicContent() {
        Column(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            val state = rememberLazyListState()

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = Theme.shadow.v7
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icon = Icons.ArrowBack,
                        onClick = ::onBack
                    )

                    HorizontalScrollContainer(state = state, modifier = Modifier.weight(1f)) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().background(Theme.color.surface),
                            state = state,
                            horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(items = MessageType.entries, key = { it }) { type ->
                                val manager: MessageManager<*> = DataSourceInformation.manager(type)
                                val isSelected = currentManager == manager

                                val accentColor = when (manager.level) {
                                    APILevel.TODO -> Theme.color.disabledContainer
                                    APILevel.ALPHA -> Colors.Pink4
                                    APILevel.BETA -> Colors.Red4
                                    APILevel.RC -> Colors.Orange4
                                    APILevel.STABLE -> Colors.Green4
                                }
                                val borderColor = if (isSelected) Theme.color.onSurface.copy(alpha = 0.8f) else accentColor.copy(alpha = 0.8f)
                                val shape = Theme.shape.v7

                                Box(
                                    modifier = Modifier
                                        .clip(shape)
                                        .condition(isSelected) { background(accentColor.copy(alpha = 0.35f)) }
                                        .border(Theme.border.v6, borderColor, shape)
                                        .clickable(enabled = !isNavigating) { onNavigate(manager) }
                                        .padding(Theme.padding.e),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        icon = manager.icon,
                                        color = Colors.Unspecified,
                                        tip = manager.name
                                    )
                                }
                            }
                        }
                    }

                    Icon(
                        icon = Icons.Settings,
                        onClick = {
                            with(currentManager) { openSettings() }
                        }
                    )
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
                    onRefresh = { requestNewData() },
                    onLoading = { requestMoreData() },
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