package love.yinlin.tpl.weibo

import androidx.compose.runtime.Stable
import love.yinlin.common.TPProxy
import love.yinlin.uri.Uri

@Stable
object WeiboUrl {
    private const val BASE = "https://m.weibo.cn"
    private const val API_BASE = "https://m.weibo.cn/api"
    private const val CONTAINER_BASE = "$API_BASE/container"
    fun searchUser(key: String): String = TPProxy.proxy("$CONTAINER_BASE/getIndex?containerid=100103type%3D3%26q=${Uri.encodeUri(key)}&page_type=searchall")
    fun searchTopic(name: String): String = TPProxy.proxy("$BASE/search?containerid=231522type=1&q=$name")
    fun userDetails(uid: String): String = TPProxy.proxy("$CONTAINER_BASE/getIndex?type=uid&value=$uid&containerid=107603$uid")
    fun userInfo(uid: String): String = TPProxy.proxy("$CONTAINER_BASE/getIndex?type=uid&value=$uid")
    fun weiboComments(uid: String): String = TPProxy.proxy("$BASE/comments/hotflow?id=$uid&mid=$uid")
    fun weiboDetails(id: String): String = TPProxy.proxy("$BASE/statuses/show?id=$id")
    fun userAlbum(uid: String): String = TPProxy.proxy("$CONTAINER_BASE/getIndex?type=uid&value=$uid&containerid=107803$uid")
    fun albumPics(containerId: String, page: Int, limit: Int): String = TPProxy.proxy("$CONTAINER_BASE/getSecond?containerid=$containerId&count=$limit&page=$page")
    fun chaohua(page: Int): String = TPProxy.proxy("https://weibo.com/ajax_proxy/chaohua/page?flowId=10080848e33cc4065cd57c5503c2419cdea983_-_feed&page=$page")
    fun href(route: String) = TPProxy.proxy("$BASE$route")
    val xsrfConfig: String get() = TPProxy.proxy("$API_BASE/config")
    val genvisitor2: String get() = TPProxy.proxy("https://visitor.passport.weibo.cn/visitor/genvisitor2")
}