package love.yinlin.tpl.weibo

import androidx.compose.runtime.Stable
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import love.yinlin.common.TPProxy
import love.yinlin.data.information.UnifiedPicture
import love.yinlin.data.weibo.*
import love.yinlin.extension.*
import love.yinlin.foundation.NetClient
import love.yinlin.uri.Uri

@Stable
object WeiboAPI {
    // ######## 提取 ########
    @Stable
    private object Fetcher {
        fun extractUserInfo(user: JsonObject): WeiboUserInfo {
            // 提取名称和头像
            val userId = user["id"].String
            val userName = user["screen_name"].String
            val avatar = TPProxy.proxyRes(user["avatar_hd"].String)
            return WeiboUserInfo(
                id = userId,
                name = userName,
                avatar = avatar
            )
        }

        fun extractWeibo(rawBlog: JsonObject): Weibo {
            var blog = rawBlog
            // 提取ID
            val blogId = blog["id"].String
            val userInfo = extractUserInfo(blog.obj("user"))
            // 提取时间
            val time = WeiboDate.convert(blog["created_at"].String)
            // 提取IP
            val location = blog["region_name"]?.StringNull?.let {
                val index = it.indexOf(' ')
                if (index != -1) it.substring(index + 1) else it
            } ?: "IP未知"
            // 提取内容
            val content = blog["text"].String
            // 提取数据
            val commentNum = blog["comments_count"].Int
            val likeNum = blog["attitudes_count"].Int
            val repostNum = blog["reposts_count"].Int
            blog = blog["retweeted_status"]?.Object ?: blog // 转发微博
            // 图片微博
            val pictures: MutableList<UnifiedPicture> = []
            if ("pics" in blog) {
                for (picItem in blog.arr("pics")) {
                    val pic = picItem.Object
                    pictures += UnifiedPicture(
                        image = TPProxy.proxyRes(pic["url"].String),
                        source = TPProxy.proxyRes(pic.obj("large")["url"].String)
                    )
                }
            } else if ("page_info" in blog) {
                val pageInfo = blog.obj("page_info")
                if (pageInfo["type"].String == "video") {
                    val urls = pageInfo.obj("urls")
                    val videoUrl = if ("mp4_720p_mp4" in urls) urls["mp4_720p_mp4"].String
                    else if ("mp4_hd_mp4" in urls) urls["mp4_hd_mp4"].String
                    else urls["mp4_ld_mp4"].String
                    val videoPicUrl = pageInfo.obj("page_pic")["url"].String
                    pictures += UnifiedPicture(
                        image = TPProxy.proxyRes(videoPicUrl),
                        source = TPProxy.proxyRes(videoPicUrl),
                        video = TPProxy.proxyRes(videoUrl)
                    )
                }
            }
            return Weibo(
                id = blogId,
                user = userInfo,
                time = time,
                location = location,
                content = content,
                data = WeiboData(commentNum, likeNum, repostNum),
                pictures = pictures
            )
        }

        fun extractComment(card: JsonObject): WeiboComment {
            val commentId = card["id"].String
            // 提取名称和头像
            val userInfo = extractUserInfo(card.obj("user"))
            // 提取时间
            val time = WeiboDate.convert(card["created_at"].String)
            // 提取IP
            val location = card["source"]?.StringNull?.removePrefix("来自") ?: "IP未知"
            // 提取内容
            val content = card["text"].String
            // 带图片
            val pictures = if ("pic" in card) {
                card.obj("pic").let {
                    [UnifiedPicture(
                        image = TPProxy.proxyRes(it["url"].String),
                        source = TPProxy.proxyRes(it.obj("large")["url"].String)
                    )]
                }
            } else []
            // 楼中楼
            val subComments: MutableList<WeiboSubComment> = []
            val comments = card["comments"]
            if (comments as? JsonArray != null) {
                for (subCard in comments) {
                    val subCardObj = subCard.Object
                    subComments += WeiboSubComment(
                        id = subCardObj["id"].String,
                        user = extractUserInfo(subCardObj.obj("user")),
                        time = WeiboDate.convert(subCardObj["created_at"].String),
                        location = subCardObj["source"]?.StringNull?.removePrefix("来自") ?: "IP未知",
                        content = subCardObj["text"].String
                    )
                }
            }
            return WeiboComment(
                id = commentId,
                user = userInfo,
                time = time,
                location = location,
                content = content,
                pictures = pictures,
                subComments = subComments
            )
        }
    }

    private suspend inline fun <reified R : Any> weiboRequest(
        url: String,
        cookie: WeiboCookie,
        headers: Array<Pair<String, String>> = [HttpHeaders.Referrer to "https://m.weibo.cn"],
        crossinline onRequest: () -> Unit = {},
        crossinline onResponse: suspend (JsonObject) -> R
    ): R? = NetClient.Common.request({
        this.url = url
        this.headers = TPProxy.proxyHeader(mapOf(
            HttpHeaders.Cookie to "SUB=${cookie.sub};SUBP=${cookie.subp};XSRF-TOKEN=${cookie.xsrfToken}",
            *headers
        ))
        onRequest()
    }, onResponse)

    // ######## 相关API ########

    // 生成cookie
    suspend fun generateCookie(): WeiboCookie {
        val xsrfToken = NetClient.Common.request<ByteArray, String>({
            url = WeiboUrl.xsrfConfig
        }) {
            cookies.filter { it.name.equals("XSRF-TOKEN", ignoreCase = true) }.first { !it.value.equals("deleted", ignoreCase = true) }.value
        } ?: "fu*you"

        val [sub, subp] = NetClient.Common.request({
            url = WeiboUrl.genvisitor2
            method = HttpMethod.Post
            form = mapOf("cb" to "visitor_gray_callback")
        }) { text: String ->
            val json = text.substringAfter("(").substringBeforeLast(")").parseJson.Object
            val data = json.obj("data")
            data["sub"].String to data["subp"].String
        } ?: ("_2AkMeSKrwf8NxqwJRmvwUymjlZIh3zw_EieKoFFsrJRM3HRl-yT9yqhAgtRB6NciEEb-f-w8Zld8pGpTn4blqg02DqNuH" to "0033WrSXqPxfM72-Ws9jqgMF55529P9D9WhjLXMq867aPUPiUkd8wq4Y")

        return WeiboCookie(sub, subp, xsrfToken)
    }

    /**
     * 获取用户详细信息
     *
     * @param uid 用户ID
     */
    suspend fun requestUser(uid: String, cookie: WeiboCookie): WeiboUser? = weiboRequest(WeiboUrl.userInfo(uid), cookie) { json: JsonObject ->
        val userInfo = json.obj("data").obj("userInfo")
        val id = userInfo["id"].String
        val name = userInfo["screen_name"].String
        val avatar = TPProxy.proxyRes(userInfo["avatar_hd"].String)
        val background = TPProxy.proxyRes(userInfo["cover_image_phone"].String)
        val signature = userInfo["description"].String
        val followNum = userInfo["follow_count"].String
        val fansNum = userInfo["followers_count_str"]?.StringNull ?: userInfo["followers_count"].String
        WeiboUser(
            user = WeiboUserInfo(id, name, avatar),
            background = background,
            signature = signature,
            followNum = followNum,
            fansNum = fansNum
        )
    }

    /**
     * 获取用户相册
     *
     * @param uid 用户ID
     */
    suspend fun requestUserAlbum(uid: String, cookie: WeiboCookie): List<WeiboAlbum>? = weiboRequest(WeiboUrl.userAlbum(uid), cookie) { json: JsonObject ->
        val cards = json.obj("data").arr("cards")
        val items: MutableList<WeiboAlbum> = []
        for (item1 in cards) {
            val card = item1.Object
            if (card["itemid"].String.endsWith("albumeach")) {
                for (item2 in card.arr("card_group")) {
                    val album = item2.Object
                    if (album["card_type"].Int == 8) {
                        val containerId = Uri.parse(album["scheme"].String)!!.params["containerid"]!!
                        items += WeiboAlbum(
                            containerId = containerId,
                            title = album["title_sub"].String,
                            num = album["desc1"].String,
                            time = album["desc2"].String,
                            pic = TPProxy.proxyRes(album["pic"].String)
                        )
                    }
                }
            }
        }
        items
    }

    /**
     * 获取相册图片
     *
     * @param page 第一页是1
     * @param limit 一般是24
     */
    suspend fun requestUserAlbumPics(containerId: String, page: Int, limit: Int, cookie: WeiboCookie): WeiboAlbumPics? = weiboRequest(WeiboUrl.albumPics(containerId, page, limit), cookie) { json: JsonObject ->
        val data = json.obj("data")
        val cards = data.arr("cards")
        val pics: MutableList<UnifiedPicture> = []
        for (item1 in cards) {
            val card = item1.Object
            for (item2 in card.arr("pics")) {
                val pic = item2.Object
                pics += UnifiedPicture(
                    image = TPProxy.proxyRes(pic["pic_middle"].String),
                    source = TPProxy.proxyRes(pic["pic_ori"].String)
                )
            }
        }
        WeiboAlbumPics(
            items = pics.take(limit),
            count = data["count"].Int
        )
    }

    /**
     * 获取用户微博
     *
     * @param uid 用户ID
     */
    suspend fun requestUserWeibo(uid: String, cookie: WeiboCookie): List<Weibo>? = weiboRequest(WeiboUrl.userDetails(uid), cookie) { json: JsonObject ->
        val cards = json.obj("data").arr("cards")
        val items: MutableList<Weibo> = []
        for (item in cards) {
            val card = item.Object
            if (card["card_type"].Int != 9) continue  // 非微博类型
            items += Fetcher.extractWeibo(card.obj("mblog"))
        }
        items
    }

    /**
     * 获取微博评论
     *
     * @param id 微博ID
     */
    suspend fun requestWeiboComment(id: String, cookie: WeiboCookie): List<WeiboComment>? = weiboRequest(WeiboUrl.weiboDetails(id), cookie) { json: JsonObject ->
        val cards = json.obj("data").arr("data")
        val items: MutableList<WeiboComment> = []
        for (item in cards) items += Fetcher.extractComment(item.Object)
        items
    }

    /**
     * 搜索微博用户
     *
     * @param key 关键词
     */
    suspend fun searchUser(key: String, cookie: WeiboCookie): List<WeiboUserInfo>? = weiboRequest(WeiboUrl.searchUser(key), cookie) { json: JsonObject ->
        val cards = json.obj("data").arr("cards")
        val items: MutableList<WeiboUserInfo> = []
        for (item1 in cards) {
            val group = item1.Object
            if (group["card_type"].Int == 11) {
                for (item2 in group.arr("card_group")) {
                    val card = item2.Object
                    if (card["card_type"].Int == 10) {
                        val user = card["user"].Object
                        items += WeiboUserInfo(
                            id = user["id"].String,
                            name = user["screen_name"].String,
                            avatar = TPProxy.proxyRes(user["avatar_hd"].String)
                        )
                    }
                }
            }
        }
        items
    }

    /**
     * 请求超话
     *
     * @param page 初始是1
     */
    suspend fun requestChaohua(page: Int, cookie: WeiboCookie): List<Weibo>? = weiboRequest(
        url = WeiboUrl.chaohua(page),
        cookie = cookie,
        headers = [HttpHeaders.Referrer to "https://weibo.com", "X-Requested-With" to "XMLHttpRequest"]
    ) { json: JsonObject ->
        val items = json.arr("items")
        val weibos: MutableList<Weibo> = []
        for (item in items) {
            catching {
                val card = item.Object
                val category = card["category"].String
                if (category == "feed") {
                    val weibo = Fetcher.extractWeibo(card["data"].Object)
                    weibos += weibo
                }
            }
        }
        weibos
    }
}