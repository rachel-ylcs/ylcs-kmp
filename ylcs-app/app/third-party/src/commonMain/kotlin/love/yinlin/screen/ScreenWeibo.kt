package love.yinlin.screen

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import love.yinlin.app
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.common.WeiboGrid
import love.yinlin.compose.ui.container.RachelStatefulProvider
import love.yinlin.compose.ui.container.StatefulBox
import love.yinlin.compose.ui.container.StatefulStatus
import love.yinlin.compose.ui.floating.DialogDownload
import love.yinlin.compose.ui.floating.FAB
import love.yinlin.compose.ui.floating.FABScrollTop
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.data.weibo.Weibo
import love.yinlin.tpl.WeiboAPI

@Stable
class ScreenWeibo : Screen() {
    private val provider = RachelStatefulProvider()
    private val items = mutableStateListOf<Weibo>()
    private val gridState = LazyStaggeredGridState()

    private suspend fun requestWeibo() {
        provider.withLoading {
            val users = app.config.weiboUsers.map { it.id }
            if (users.isEmpty()) false
            else {
                // 微博需要获取subCookie
                if (WeiboAPI.weiboCookie == null) WeiboAPI.weiboCookie = WeiboAPI.generateWeiboCookie()

                items.clear()
                for (id in users) {
                    WeiboAPI.getUserWeibo(id)?.let { result ->
                        items += result
                        items.sortDescending()
                        if (provider.isLoading) provider.status = StatefulStatus.Content
                    }
                    gridState.requestScrollToItem(0)
                }
                require(!provider.isLoading) { WeiboAPI.weiboCookie = null }
                true
            }
        }
    }

    override val title: String = "微博"

    override suspend fun initialize() {
        requestWeibo()
    }

    @Composable
    override fun RowScope.RightActions() {
        LoadingIcon(icon = Icons.Refresh, onClick = ::requestWeibo)
        Icon(icon = Icons.AccountCircle, tip = "关注列表", onClick = {
            navigate(::ScreenWeiboFollows)
        })
    }

    @Composable
    override fun Content() {
        StatefulBox(
            provider = provider,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            WeiboGrid(
                state = gridState,
                items = items,
                modifier = Modifier.fillMaxSize(),
                downloadDialog = downloadDialog
            )
        }
    }

    override val fab: FAB = FABScrollTop(gridState)

    private val downloadDialog = this land DialogDownload()
}