package love.yinlin.compose.ds

import androidx.compose.runtime.Stable
import love.yinlin.common.*
import love.yinlin.compose.ui.floating.DialogChoice
import love.yinlin.compose.ui.floating.DialogDownload

@Stable
object DataSourceInformation {
    @PublishedApi
    internal val managers = mapOf(
        MessageType.Weibo to WeiboManager(),
        MessageType.Chaohua to ChaohuaManager(),
        MessageType.Bilibili to BilibiliManager(),
        MessageType.Douyin to DouyinManager(),
        MessageType.RedBook to RedBookManager()
    )

    inline fun <reified M : MessageManager<*>> manager(type: MessageType): M = managers[type] as M

    // 公共下载窗口
    // 可通过 Land 落地任何微博相关页面
    val CommonDownloadDialog = DialogDownload()
    val ChoiceDialog = DialogChoice.ByDynamicList()
}