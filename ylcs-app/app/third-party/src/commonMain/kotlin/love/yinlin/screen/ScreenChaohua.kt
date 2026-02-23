package love.yinlin.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.common.WeiboCard
import love.yinlin.compose.ui.container.RachelStatefulProvider
import love.yinlin.compose.ui.container.StatefulBox
import love.yinlin.compose.ui.floating.DialogDownload
import love.yinlin.compose.ui.floating.FAB
import love.yinlin.compose.ui.floating.FABAction
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.layout.PaginationStaggeredGrid
import love.yinlin.data.weibo.Weibo
import love.yinlin.tpl.WeiboAPI

@Stable
class ScreenChaohua : Screen() {
    private val provider = RachelStatefulProvider()
    private var items by mutableRefStateOf(emptyList<Weibo>())
    private val gridState = LazyStaggeredGridState()
    private var sinceId: Long = 0L
    private var canLoading by mutableStateOf(false)

    private suspend fun requestNewData(loading: Boolean) {
        provider.withLoading(loading) {
            canLoading = false

            // 微博需要获取subCookie
            if (WeiboAPI.weiboCookie == null) WeiboAPI.weiboCookie = WeiboAPI.generateWeiboCookie()

            val result = WeiboAPI.extractChaohua(0L)
            require(result != null) { WeiboAPI.weiboCookie = null }
            val (data, newSinceId) = result
            sinceId = newSinceId
            canLoading = newSinceId != 0L
            items = data
            data.isNotEmpty()
        }
    }

    private suspend fun requestMoreData() {
        val result = WeiboAPI.extractChaohua(sinceId)
        if (result != null) {
            val (data, newSinceId) = result
            sinceId = newSinceId
            canLoading = newSinceId != 0L
            items += data
        }
        else WeiboAPI.weiboCookie = null
    }

    override val title: String = "超话"

    override suspend fun initialize() {
        requestNewData(true)
    }

    @Composable
    override fun Content() {
        StatefulBox(
            provider = provider,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            PaginationStaggeredGrid(
                items = items,
                key = { it.id },
                columns = StaggeredGridCells.Adaptive(Theme.size.cell1),
                state = gridState,
                canRefresh = true,
                canLoading = canLoading,
                onRefresh = { requestNewData(false) },
                onLoading = { requestMoreData() },
                modifier = Modifier.fillMaxSize(),
                contentPadding = Theme.padding.eValue,
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
                verticalItemSpacing = Theme.padding.e
            ) { weibo ->
                WeiboCard(weibo = weibo, modifier = Modifier.fillMaxWidth(), downloadDialog = downloadDialog)
            }
        }
    }

    override val fab: FAB = object : FAB() {
        private val isScrollTop: Boolean by derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }

        override val action: FABAction = FABAction(
            iconProvider = { if (isScrollTop) Icons.Refresh else Icons.ArrowUpward },
            onClick = {
                if (isScrollTop) requestNewData(true)
                else gridState.animateScrollToItem(0)
            }
        )
    }

    private val downloadDialog = this land DialogDownload()
}