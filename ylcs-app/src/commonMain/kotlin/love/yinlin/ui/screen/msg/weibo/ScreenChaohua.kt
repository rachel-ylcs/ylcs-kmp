package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.panpf.sketch.ability.bindPauseLoadWhenScrolling
import love.yinlin.AppModel
import love.yinlin.api.WeiboAPI
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.Data
import love.yinlin.data.weibo.Weibo
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.PaginationStaggeredGrid
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.screen.CommonSubScreen

@Stable
class ScreenChaohua(model: AppModel) : CommonSubScreen(model) {
    private var state by mutableStateOf(BoxState.EMPTY)
    private var items by mutableStateOf(emptyList<Weibo>())
    private val listState = LazyStaggeredGridState()
    private var sinceId: Long = 0L
    private var canLoading by mutableStateOf(false)

    private suspend fun requestNewData() {
        if (state != BoxState.LOADING) {
            state = BoxState.LOADING
            canLoading = false
            val result = WeiboAPI.extractChaohua(0L)
            state = if (result is Data.Success) {
                val (data, newSinceId) = result.data
                sinceId = newSinceId
                canLoading = newSinceId != 0L
                items = data
                if (data.isEmpty()) BoxState.EMPTY else BoxState.CONTENT
            }
            else BoxState.NETWORK_ERROR
        }
    }

    private suspend fun requestMoreData() {
        val result = WeiboAPI.extractChaohua(sinceId)
        if (result is Data.Success) {
            val (data, newSinceId) = result.data
            sinceId = newSinceId
            canLoading = newSinceId != 0L
            items += data
        }
    }

    override val title: String = "超话"

    override suspend fun initialize() {
        requestNewData()
    }

    @Composable
    override fun SubContent(device: Device) {
        CompositionLocalProvider(LocalWeiboProcessor provides msgPart.processor) {
            StatefulBox(
                state = state,
                modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
            ) {
                bindPauseLoadWhenScrolling(listState)
                PaginationStaggeredGrid(
                    items = items,
                    key = { it.id },
                    columns = StaggeredGridCells.Adaptive(ThemeValue.Size.CardWidth),
                    state = listState,
                    canRefresh = true,
                    canLoading = canLoading,
                    onRefresh = { requestNewData() },
                    onLoading = { requestMoreData() },
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = ThemeValue.Padding.EqualValue,
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                    verticalItemSpacing = ThemeValue.Padding.EqualSpace
                ) { weibo ->
                    WeiboCard(
                        weibo = weibo,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    private val isScrollTop: Boolean by derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 }

    override val fabIcon: ImageVector get() = if (isScrollTop) Icons.Outlined.Refresh else Icons.Outlined.ArrowUpward

    override suspend fun onFabClick() {
        if (isScrollTop) launch { requestNewData() }
        else listState.animateScrollToItem(0)
    }
}