package love.yinlin.tpl.weibo

import androidx.compose.runtime.Stable
import love.yinlin.common.TPProxy
import love.yinlin.uri.Uri

@Stable
object WeiboUrl {
    fun searchUser(key: String): String = TPProxy.proxy("https://m.weibo.cn/api/container/getIndex?containerid=100103type%3D3%26q=${Uri.encodeUri(key)}&page_type=searchall")
    fun searchTopic(name: String): String = TPProxy.proxy("https://m.weibo.cn/search?containerid=231522type=1&q=$name")
    fun userDetails(uid: String): String = TPProxy.proxy("https://m.weibo.cn/api/container/getIndex?type=uid&value=$uid&containerid=107603$uid")
    fun userInfo(uid: String): String = TPProxy.proxy("https://m.weibo.cn/api/container/getIndex?type=uid&value=$uid")
    fun weiboDetails(uid: String): String = TPProxy.proxy("https://m.weibo.cn/comments/hotflow?id=$uid&mid=$uid")
    fun userAlbum(uid: String): String = TPProxy.proxy("https://m.weibo.cn/api/container/getIndex?type=uid&value=$uid&containerid=107803$uid")
    fun albumPics(containerId: String, page: Int, limit: Int): String = TPProxy.proxy("https://m.weibo.cn/api/container/getSecond?containerid=$containerId&count=$limit&page=$page")
    fun chaohua(page: Int): String = TPProxy.proxy("https://weibo.com/ajax_proxy/chaohua/page?flowId=10080848e33cc4065cd57c5503c2419cdea983_-_feed&page=$page")
    fun href(route: String) = TPProxy.proxy("https://m.weibo.cn$route")
    val xsrfConfig: String get() = TPProxy.proxy("https://m.weibo.cn/api/config")
    val genvisitor2: String get() = TPProxy.proxy("https://visitor.passport.weibo.cn/visitor/genvisitor2")
}