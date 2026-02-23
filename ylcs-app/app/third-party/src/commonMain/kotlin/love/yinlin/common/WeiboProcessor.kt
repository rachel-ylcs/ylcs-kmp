package love.yinlin.common

import androidx.compose.runtime.Stable
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.data.compose.Picture
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboUserInfo

@Stable
interface WeiboProcessor {
    fun BasicScreen.onWeiboClick(weibo: Weibo)
    fun BasicScreen.onWeiboAvatarClick(info: WeiboUserInfo)
    fun BasicScreen.onWeiboLinkClick(arg: String)
    fun BasicScreen.onWeiboTopicClick(arg: String)
    fun BasicScreen.onWeiboAtClick(arg: String)
    fun BasicScreen.onWeiboPicClick(pics: List<Picture>, current: Int)
    fun BasicScreen.onWeiboVideoClick(pic: Picture)
}