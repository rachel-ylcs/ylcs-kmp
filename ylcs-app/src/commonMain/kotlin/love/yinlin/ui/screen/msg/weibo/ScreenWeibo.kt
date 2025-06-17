package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.AppModel
import love.yinlin.api.WeiboAPI
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.data.Data
import love.yinlin.data.weibo.Weibo
import love.yinlin.platform.app
import love.yinlin.ui.component.layout.ActionScope
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.screen.CommonSubScreen

@Stable
class ScreenWeibo(model: AppModel) : CommonSubScreen(model) {
    private var state by mutableStateOf(BoxState.EMPTY)
    private var items = mutableStateListOf<Weibo>()
    private val listState = LazyStaggeredGridState()

    private suspend fun requestWeibo() {
        if (state != BoxState.LOADING) {
            val users = app.config.weiboUsers.map { it.id }
            if (users.isEmpty()) state = BoxState.EMPTY
            else {
                state = BoxState.LOADING
                items.clear()
                for (id in users) {
                    val result = WeiboAPI.getUserWeibo(id)
                    if (result is Data.Success) {
                        items += result.data
                        items.sortDescending()
                        if (state == BoxState.LOADING) state = BoxState.CONTENT
                        listState.scrollToItem(0)
                    }
                }
                if (state == BoxState.LOADING) state = BoxState.NETWORK_ERROR
            }
        }
    }

    override val title: String = "微博"

    @Composable
    override fun ActionScope.RightActions() {
        Action(Icons.Outlined.AccountCircle) {
            navigate<ScreenWeiboFollows>()
        }
    }

    override suspend fun initialize() {
        requestWeibo()
    }

    @Composable
    override fun SubContent(device: Device) {
        CompositionLocalProvider(LocalWeiboProcessor provides msgPart.processor) {
            StatefulBox(
                state = state,
                modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
            ) {
                WeiboGrid(
                    state = listState,
                    items = items,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private val isScrollTop: Boolean by derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 }

    override val fabIcon: ImageVector get() = if (isScrollTop) Icons.Outlined.Refresh else Icons.Outlined.ArrowUpward

    override suspend fun onFabClick() {
        if (isScrollTop) launch { requestWeibo() }
        else listState.animateScrollToItem(0)
    }
}