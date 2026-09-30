package love.yinlin.compose.ds

import androidx.compose.runtime.Stable
import love.yinlin.common.ChaohuaManager
import love.yinlin.common.DouyinManager
import love.yinlin.common.MessageType
import love.yinlin.common.WeiboManager
import love.yinlin.compose.ui.floating.DialogChoice
import love.yinlin.compose.ui.floating.DialogDownload
import love.yinlin.tpl.weibo.WeiboAPI
import love.yinlin.tpl.weibo.WeiboCookie

@Stable
object DataSourceInformation {
    val managers = mapOf(
        MessageType.Weibo to WeiboManager(),
        MessageType.Chaohua to ChaohuaManager(),
        MessageType.Douyin to DouyinManager(),
    )

    // 公共下载窗口
    // 可通过 Land 落地任何微博相关页面
    val CommonDownloadDialog = DialogDownload()
    val ChoiceDialog = DialogChoice.ByDynamicList()

    private var weiboCookie: WeiboCookie? = null

    suspend fun fetchWeiboCookie(): WeiboCookie {
        val oldCookie = weiboCookie
        if (oldCookie == null) {
            val cookie = WeiboAPI.generateCookie()
            weiboCookie = cookie
            return cookie
        }
        return oldCookie
    }

    fun resetWeiboCookies() {
        weiboCookie = null
    }
}