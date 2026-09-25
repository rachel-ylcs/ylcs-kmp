package love.yinlin.compose.ds

import androidx.compose.runtime.Stable
import love.yinlin.common.ChaohuaManager
import love.yinlin.common.WeiboManager

@Stable
object DataSourceInformation {
    val managers = [
        WeiboManager(),
        ChaohuaManager()
    ]
}