package love.yinlin.common

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.compose.ds.DataSourceInformation
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.icon.Icons2
import love.yinlin.screen.ScreenChaohuaSettings
import love.yinlin.tpl.weibo.WeiboAPI

@Stable
class ChaohuaManager : BasicWeiboManager() {
    override val name: String = "超话"
    override val icon: ImageVector = Icons2.Chaohua
    override val level: APILevel = APILevel.Stable

    private var currentPage: Int = 1

    override suspend fun onNewData(flushContent: () -> Unit): Boolean {
        val cookie = DataSourceInformation.fetchWeiboCookie()

        val result = WeiboAPI.requestChaohua(1, cookie)
        require(!result.isNullOrEmpty()) { DataSourceInformation.resetWeiboCookies() }
        currentPage = 1
        items = result
        // 超话始终都能加载新的内容
        return true
    }

    override suspend fun onMoreData(): Boolean {
        val cookie = DataSourceInformation.fetchWeiboCookie()

        val newPage = currentPage + 1
        val result = WeiboAPI.requestChaohua(newPage, cookie)
        require(result != null) { DataSourceInformation.resetWeiboCookies() }
        currentPage = newPage
        items = items + result
        return true
    }

    override fun BasicScreen.openSettings() = navigate(::ScreenChaohuaSettings)
}