package love.yinlin.common

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.icon.Icons2
import love.yinlin.screen.ScreenChaohuaSettings
import love.yinlin.tpl.weibo.WeiboAPI

@Stable
class ChaohuaManager : BasicWeiboManager() {
    override val name: String = "超话"
    override val icon: ImageVector = Icons2.Chaohua
    override val level: APILevel = APILevel.STABLE

    private var currentPage: Int = 1

    override suspend fun onNewData(flushContent: () -> Unit): Boolean {
        val cookie = fetchWeiboCookie()

        val result = WeiboAPI.requestChaohua(1, cookie)
        require(!result.isNullOrEmpty()) { resetWeiboCookies() }
        currentPage = 1
        items = result
        // 超话始终都能加载新的内容
        return true
    }

    override suspend fun onMoreData(): Boolean {
        val cookie = fetchWeiboCookie()

        val newPage = currentPage + 1
        val result = WeiboAPI.requestChaohua(newPage, cookie)
        require(result != null) { resetWeiboCookies() }
        currentPage = newPage
        items = items + result
        return true
    }

    override fun BasicScreen.openSettings() = navigate(::ScreenChaohuaSettings)
}