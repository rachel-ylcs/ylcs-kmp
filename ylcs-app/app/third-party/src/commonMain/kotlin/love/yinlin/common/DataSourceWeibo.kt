package love.yinlin.common

import androidx.compose.runtime.Stable
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.screen.DataSource
import love.yinlin.data.compose.Picture
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.screen.ScreenImagePreview
import love.yinlin.screen.ScreenVideo
import love.yinlin.screen.ScreenWeiboDetails
import love.yinlin.screen.ScreenWeiboUser
import love.yinlin.screen.navigateScreenWebPage
import love.yinlin.tpl.WeiboAPI

@Stable
object DataSourceWeibo : DataSource {
    // 当前微博
    var currentWeibo: Weibo? = null
    // 是否正在查找用户
    private var isSearchingUser: Boolean = false

    // 微博处理器
    val processor = object : WeiboProcessor {
        override fun BasicScreen.onWeiboClick(weibo: Weibo) {
            currentWeibo = weibo
            navigate(::ScreenWeiboDetails)
        }

        override fun BasicScreen.onWeiboAvatarClick(info: WeiboUserInfo) {
            navigate(::ScreenWeiboUser, info.id)
        }

        override fun BasicScreen.onWeiboLinkClick(arg: String) = navigateScreenWebPage(arg)

        override fun BasicScreen.onWeiboTopicClick(arg: String) = navigateScreenWebPage(arg)

        override fun BasicScreen.onWeiboAtClick(arg: String) {
            launch {
                val name = arg.substringAfterLast("/n/")
                val user = when {
                    name.isEmpty() -> null
                    isSearchingUser -> return@launch
                    else -> {
                        isSearchingUser = true
                        WeiboAPI.searchWeiboUser(name)?.find { it.name == name }.also {
                            isSearchingUser = false
                        }
                    }
                }
                if (user == null) navigateScreenWebPage(arg)
                else navigate(::ScreenWeiboUser, user.id)
            }
        }

        override fun BasicScreen.onWeiboPicClick(pics: List<Picture>, current: Int) {
            navigate(::ScreenImagePreview, pics, current)
        }

        override fun BasicScreen.onWeiboVideoClick(pic: Picture) {
            navigate(::ScreenVideo, pic.video)
        }
    }

    override fun onDataSourceClean() {
        currentWeibo = null
    }
}