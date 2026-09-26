package love.yinlin.common

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.app
import love.yinlin.compose.ds.DataSourceInformation
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.icon.Icons2
import love.yinlin.extension.then
import love.yinlin.screen.ScreenWeiboSettings
import love.yinlin.tpl.weibo.WeiboAPI

@Stable
class WeiboManager : BasicWeiboManager() {
    override val name: String = "微博"
    override val icon: ImageVector = Icons2.Weibo

    override suspend fun onNewData(flushContent: () -> Unit): Boolean {
        val users = app.config.weiboUsers.map { it.id }
        if (users.isEmpty()) return false

        val cookie = DataSourceInformation.fetchWeiboCookie()

        // 批量更新
        items = []
        for (id in users) {
            WeiboAPI.requestUserWeibo(id, cookie)?.then { result ->
                items = (items + result).sortedDescending()
                flushContent()
            }
            gridState.requestScrollToItem(0)
        }
        require(items.isNotEmpty()) { DataSourceInformation.resetWeiboCookies() }
        // 微博只能加载一页，不能Loading
        return false
    }

    override fun BasicScreen.openSettings() = navigate(::ScreenWeiboSettings)
}